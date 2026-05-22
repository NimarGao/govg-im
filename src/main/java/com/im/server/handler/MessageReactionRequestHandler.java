package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.MessageReactionRequestPacket;
import com.im.protocol.command.MessageReactionResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

@ChannelHandler.Sharable
public class MessageReactionRequestHandler extends SimpleChannelInboundHandler<MessageReactionRequestPacket> {

    public static final MessageReactionRequestHandler INSTANCE = new MessageReactionRequestHandler();

    private MessageReactionRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageReactionRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String fromUserId = session.getUserId();
        String fromUserName = session.getUserName();
        String msgId = packet.getMsgId();
        String emoji = packet.getEmoji();
        String toUserId = packet.getToUserId();
        String toGroupId = packet.getToGroupId();
        String action = packet.getAction(); // "add" | "remove"

        System.out.println("Reaction Signal: User [" + fromUserId + "] wants to " + action + " emoji [" + emoji + "] on message [" + msgId + "]");

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        if (redisTemplate != null) {
            String redisKey = "im:message:reactions:" + msgId;
            if ("add".equalsIgnoreCase(action)) {
                redisTemplate.opsForHash().put(redisKey, fromUserId, emoji);
            } else if ("remove".equalsIgnoreCase(action)) {
                redisTemplate.opsForHash().delete(redisKey, fromUserId);
            }
        }

        // Construct response packet
        MessageReactionResponsePacket responsePacket = new MessageReactionResponsePacket(
                msgId, fromUserId, fromUserName, emoji, action, toUserId, toGroupId
        );

        // Broadcast to Redis channel for distributed environment
        try {
            if (redisTemplate != null) {
                JSONObject payload = new JSONObject();
                payload.put("command", Command.MESSAGE_REACTION_RESPONSE);
                payload.put("msgId", msgId);
                payload.put("fromUserId", fromUserId);
                payload.put("fromUserName", fromUserName);
                payload.put("emoji", emoji);
                payload.put("action", action);
                payload.put("toUserId", toUserId);
                payload.put("toGroupId", toGroupId);

                redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                System.out.println("Distributed Reaction: Route payload sent for message [" + msgId + "]");
                return;
            }
        } catch (Exception e) {
            System.err.println("Redis cluster services failed. Falling back to local delivery for reaction signal:");
            e.printStackTrace();
        }

        // Standalone fallback
        if (toGroupId != null && !toGroupId.isEmpty()) {
            Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
            if (memberIds != null) {
                for (String memberId : memberIds) {
                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        channelGroup.writeAndFlush(responsePacket);
                    }
                }
            }
        } else if (toUserId != null && !toUserId.isEmpty()) {
            ChannelGroup recipientGroup = SessionUtil.getChannelGroup(toUserId);
            if (recipientGroup != null && !recipientGroup.isEmpty()) {
                recipientGroup.writeAndFlush(responsePacket);
            }
            ChannelGroup senderGroup = SessionUtil.getChannelGroup(fromUserId);
            if (senderGroup != null && !senderGroup.isEmpty()) {
                senderGroup.writeAndFlush(responsePacket);
            }
        }
    }
}
