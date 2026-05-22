package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.AckPacket;
import com.im.protocol.command.MessageRequestPacket;
import com.im.protocol.command.MessageResponsePacket;
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
public class MessageRequestHandler extends SimpleChannelInboundHandler<MessageRequestPacket> {

    public static final MessageRequestHandler INSTANCE = new MessageRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageRequestPacket messageRequestPacket) {
        // 1. Get current session
        Session session = SessionUtil.getSession(ctx.channel());
        String toUserId = messageRequestPacket.getToUserId();

        // 2. Persistence log simulation
        System.out.println("Persistence: Saving message " + messageRequestPacket.getMsgId() + " from " + session.getUserId());

        // 3. Persist WebSocket single message into MySQL
        try {
            com.im.mapper.MessageMapper messageMapper = SpringContextHolder.getBean(com.im.mapper.MessageMapper.class);
            if (messageMapper != null) {
                com.im.entity.Message message = new com.im.entity.Message();
                message.setMsgId(messageRequestPacket.getMsgId());
                message.setFromUserId(session.getUserId());
                message.setToUserId(toUserId);
                message.setContent(messageRequestPacket.getMessage());
                message.setType(1); // 1: Single
                message.setMsgType(messageRequestPacket.getMsgType() != null ? messageRequestPacket.getMsgType() : 1);
                message.setStatus(0); // Sent
                message.setCreateTime(new java.util.Date());
                message.setQuoteMsgId(messageRequestPacket.getQuoteMsgId());
                message.setQuoteSender(messageRequestPacket.getQuoteSender());
                message.setQuoteContent(messageRequestPacket.getQuoteContent());
                messageMapper.insert(message);
                System.out.println("Persistence: Message " + messageRequestPacket.getMsgId() + " successfully saved to MySQL.");
            }
        } catch (Exception ex) {
            System.err.println("Failed to persist WebSocket message to MySQL: " + ex.getMessage());
        }

        // 4. Send ACK back to the Sender immediately to trigger client-side "Delivered" indicator
        if (messageRequestPacket.getMsgId() != null) {
            AckPacket ackPacket = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
            ctx.channel().writeAndFlush(ackPacket);
        }

        // 4. Construct payload for distribution
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("toUserId", toUserId);
        jsonPayload.put("fromUserId", session.getUserId());
        jsonPayload.put("fromUserName", session.getUserName());
        jsonPayload.put("message", messageRequestPacket.getMessage());
        jsonPayload.put("msgId", messageRequestPacket.getMsgId());
        jsonPayload.put("msgType", messageRequestPacket.getMsgType() != null ? messageRequestPacket.getMsgType() : 1);
        jsonPayload.put("quoteMsgId", messageRequestPacket.getQuoteMsgId());
        jsonPayload.put("quoteSender", messageRequestPacket.getQuoteSender());
        jsonPayload.put("quoteContent", messageRequestPacket.getQuoteContent());

        try {
            RedisService redisService = SpringContextHolder.getBean(RedisService.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            // 5. Query global online session status via Redis
            String globalSession = redisService != null ? redisService.getSession(toUserId) : null;

            if (globalSession != null) {
                // 6A. Target is GLOBALLY ONLINE: Publish to Redis Single Msg channel to route across Netty nodes
                if (redisTemplate != null) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, jsonPayload.toJSONString());
                    System.out.println("Distributed Single Route Match: Message " + messageRequestPacket.getMsgId() + " published to Redis channel for User [" + toUserId + "]");
                }
            } else {
                // 6B. Target is GLOBALLY OFFLINE: Cache inside Redis offline message list
                if (redisTemplate != null) {
                    String offlineKey = "im:offline:messages:" + toUserId;
                    redisTemplate.opsForList().rightPush(offlineKey, jsonPayload.toJSONString());
                    System.out.println("Offline Cache: User [" + toUserId + "] is globally offline. Message cached in Redis list.");
                }
            }
        } catch (Exception e) {
            System.err.println("Redis cluster services failed. Falling back to simple single-node local delivery:");
            e.printStackTrace();

            // Local fallback logic (for standalone or Redis connection loss)
            ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                MessageResponsePacket messageResponsePacket = new MessageResponsePacket();
                messageResponsePacket.setFromUserId(session.getUserId());
                messageResponsePacket.setFromUserName(session.getUserName());
                messageResponsePacket.setMessage(messageRequestPacket.getMessage());
                messageResponsePacket.setMsgId(messageRequestPacket.getMsgId());
                messageResponsePacket.setMsgType(messageRequestPacket.getMsgType());
                messageResponsePacket.setQuoteMsgId(messageRequestPacket.getQuoteMsgId());
                messageResponsePacket.setQuoteSender(messageRequestPacket.getQuoteSender());
                messageResponsePacket.setQuoteContent(messageRequestPacket.getQuoteContent());
                channelGroup.writeAndFlush(messageResponsePacket);
                System.out.println("Standalone Fallback: Dispatched locally to online User [" + toUserId + "]");
            } else {
                System.err.println("Standalone Fallback Failed: User [" + toUserId + "] is totally offline locally.");
            }
        }
    }
}
