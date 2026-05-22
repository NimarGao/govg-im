package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.VoiceCallRequestPacket;
import com.im.protocol.command.VoiceCallResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class VoiceCallRequestHandler extends SimpleChannelInboundHandler<Object> {

    public static final VoiceCallRequestHandler INSTANCE = new VoiceCallRequestHandler();

    private VoiceCallRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

        if (msg instanceof VoiceCallRequestPacket) {
            VoiceCallRequestPacket packet = (VoiceCallRequestPacket) msg;
            String callId = packet.getCallId();
            String fromUserId = packet.getFromUserId();
            String toUserId = packet.getToUserId();
            String callType = packet.getCallType();

            System.out.println("Call Request: [" + fromUserId + "] calling [" + toUserId + "] with type [" + callType + "] (Call ID: " + callId + ")");

            // Check if peer is already in a call (busy)
            boolean isCallerBusy = false;
            boolean isRecipientBusy = false;

            if (redisTemplate != null) {
                isCallerBusy = redisTemplate.hasKey("im:user:call_state:" + fromUserId);
                isRecipientBusy = redisTemplate.hasKey("im:user:call_state:" + toUserId);
            }

            if (isCallerBusy || isRecipientBusy) {
                // Instantly reply with busy status to the caller
                VoiceCallResponsePacket busyResponse = new VoiceCallResponsePacket(callId, toUserId, fromUserId, "busy");
                ctx.channel().writeAndFlush(busyResponse);
                System.out.println("Call Refused: User busy. CallerBusy: " + isCallerBusy + ", RecipientBusy: " + isRecipientBusy);
                return;
            }

            // Lock both users as busy (60s dial timeout to avoid deadlock)
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("im:user:call_state:" + fromUserId, callId, 60, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set("im:user:call_state:" + toUserId, callId, 60, TimeUnit.SECONDS);
            }

            // Broadcast request packet
            try {
                if (redisTemplate != null) {
                    JSONObject payload = new JSONObject();
                    payload.put("command", Command.VOICE_CALL_REQUEST);
                    payload.put("callId", callId);
                    payload.put("fromUserId", fromUserId);
                    payload.put("toUserId", toUserId);
                    payload.put("callType", callType);

                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                    System.out.println("Distributed Signaling: Route payload sent for voice call request [" + callId + "]");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Redis cluster signaling failed. Falling back to standalone:");
                e.printStackTrace();
            }

            // Standalone Fallback
            ChannelGroup recipientGroup = SessionUtil.getChannelGroup(toUserId);
            if (recipientGroup != null && !recipientGroup.isEmpty()) {
                recipientGroup.writeAndFlush(packet);
            }

        } else if (msg instanceof VoiceCallResponsePacket) {
            VoiceCallResponsePacket packet = (VoiceCallResponsePacket) msg;
            String callId = packet.getCallId();
            String fromUserId = packet.getFromUserId();
            String toUserId = packet.getToUserId();
            String action = packet.getAction();

            System.out.println("Call Response: [" + fromUserId + "] responded with [" + action + "] to [" + toUserId + "]");

            // Handle lock releases
            if ("reject".equalsIgnoreCase(action) || "cancel".equalsIgnoreCase(action) || "hangup".equalsIgnoreCase(action) || "busy".equalsIgnoreCase(action)) {
                if (redisTemplate != null) {
                    redisTemplate.delete("im:user:call_state:" + fromUserId);
                    redisTemplate.delete("im:user:call_state:" + toUserId);
                }
            } else if ("accept".equalsIgnoreCase(action)) {
                // Prolong call session (1 hour limit)
                if (redisTemplate != null) {
                    redisTemplate.expire("im:user:call_state:" + fromUserId, 1, TimeUnit.HOURS);
                    redisTemplate.expire("im:user:call_state:" + toUserId, 1, TimeUnit.HOURS);
                }
            }

            // Broadcast response packet
            try {
                if (redisTemplate != null) {
                    JSONObject payload = new JSONObject();
                    payload.put("command", Command.VOICE_CALL_RESPONSE);
                    payload.put("callId", callId);
                    payload.put("fromUserId", fromUserId);
                    payload.put("toUserId", toUserId);
                    payload.put("action", action);

                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                    System.out.println("Distributed Signaling: Route payload sent for voice call response [" + callId + "]");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Redis cluster signaling failed. Falling back to standalone:");
                e.printStackTrace();
            }

            // Standalone Fallback
            ChannelGroup peerGroup = SessionUtil.getChannelGroup(toUserId);
            if (peerGroup != null && !peerGroup.isEmpty()) {
                peerGroup.writeAndFlush(packet);
            }
        }
    }
}
