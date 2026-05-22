package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.ReadReceiptRequestPacket;
import com.im.protocol.command.ReadReceiptResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import com.im.util.redis.RedisService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

@ChannelHandler.Sharable
public class ReadReceiptRequestHandler extends SimpleChannelInboundHandler<ReadReceiptRequestPacket> {

    public static final ReadReceiptRequestHandler INSTANCE = new ReadReceiptRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ReadReceiptRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }
        String fromUserId = session.getUserId();
        String toUserId = packet.getToUserId();
        String lastReadMsgId = packet.getLastReadMsgId();

        System.out.println("Read Receipt: User [" + fromUserId + "] marked messages from User [" + toUserId + "] as read up to " + lastReadMsgId);

        ReadReceiptResponsePacket responsePacket = new ReadReceiptResponsePacket(fromUserId, lastReadMsgId);

        try {
            RedisService redisService = SpringContextHolder.getBean(RedisService.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            String globalSession = redisService != null ? redisService.getSession(toUserId) : null;
            if (globalSession != null) {
                if (redisTemplate != null) {
                    JSONObject payload = new JSONObject();
                    payload.put("command", Command.READ_RECEIPT_RESPONSE);
                    payload.put("toUserId", toUserId);
                    payload.put("fromUserId", fromUserId);
                    payload.put("lastReadMsgId", lastReadMsgId);

                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                    System.out.println("Distributed Read Receipt Match: Route payload sent for User [" + toUserId + "]");
                }
            } else {
                System.out.println("Target user [" + toUserId + "] is globally offline. Ignoring read receipt.");
            }
        } catch (Exception e) {
            System.err.println("Redis cluster services failed. Falling back to simple standalone local delivery for read receipt:");
            e.printStackTrace();

            ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                channelGroup.writeAndFlush(responsePacket);
                System.out.println("Standalone Fallback: Read receipt dispatched locally to User [" + toUserId + "]");
            }
        }
    }
}
