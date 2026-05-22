package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.protocol.command.GroupMessageRequestPacket;
import com.im.protocol.command.GroupMessageResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.data.redis.core.StringRedisTemplate;

@ChannelHandler.Sharable
public class GroupMessageRequestHandler extends SimpleChannelInboundHandler<GroupMessageRequestPacket> {

    public static final GroupMessageRequestHandler INSTANCE = new GroupMessageRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMessageRequestPacket requestPacket) {
        // 1. Get info
        String groupId = requestPacket.getToGroupId();
        Session session = SessionUtil.getSession(ctx.channel());

        // 2. Persist WebSocket group message into MySQL
        try {
            com.im.mapper.MessageMapper messageMapper = SpringContextHolder.getBean(com.im.mapper.MessageMapper.class);
            if (messageMapper != null) {
                com.im.entity.Message message = new com.im.entity.Message();
                message.setMsgId(requestPacket.getMsgId());
                message.setFromUserId(session.getUserId());
                message.setToUserId(groupId);
                message.setContent(requestPacket.getMessage());
                message.setType(2); // 2: Group
                message.setMsgType(requestPacket.getMsgType() != null ? requestPacket.getMsgType() : 1);
                message.setStatus(0); // Sent
                message.setCreateTime(new java.util.Date());
                message.setQuoteMsgId(requestPacket.getQuoteMsgId());
                message.setQuoteSender(requestPacket.getQuoteSender());
                message.setQuoteContent(requestPacket.getQuoteContent());
                messageMapper.insert(message);
                System.out.println("Persistence: Group Message " + requestPacket.getMsgId() + " successfully saved to MySQL.");
            }
        } catch (Exception ex) {
            System.err.println("Failed to persist Group WebSocket message to MySQL: " + ex.getMessage());
        }

        // 3. Construct Broadcast Packet payload in JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("toGroupId", groupId);
        jsonObject.put("message", requestPacket.getMessage());
        jsonObject.put("fromUser", session.getUserId());
        jsonObject.put("msgId", requestPacket.getMsgId());
        jsonObject.put("msgType", requestPacket.getMsgType());
        jsonObject.put("quoteMsgId", requestPacket.getQuoteMsgId());
        jsonObject.put("quoteSender", requestPacket.getQuoteSender());
        jsonObject.put("quoteContent", requestPacket.getQuoteContent());

        // 3. Publish to Redis channel to sync across all Netty server cluster instances
        try {
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("im:route:group", jsonObject.toJSONString());
                System.out.println("Distributed Group Route Match: Published msg to Redis channel [im:route:group] for Group [" + groupId + "]");
            }
        } catch (Exception e) {
            System.err.println("Failed to publish group message to Redis PubSub, fallback to local transmission:");
            e.printStackTrace();
            
            // Fallback: local transmission (if Redis fails)
            GroupMessageResponsePacket responsePacket = new GroupMessageResponsePacket();
            responsePacket.setFromGroupId(groupId);
            responsePacket.setMessage(requestPacket.getMessage());
            responsePacket.setFromUser(session.getUserId());
            responsePacket.setMsgId(requestPacket.getMsgId());
            responsePacket.setMsgType(requestPacket.getMsgType());
            responsePacket.setQuoteMsgId(requestPacket.getQuoteMsgId());
            responsePacket.setQuoteSender(requestPacket.getQuoteSender());
            responsePacket.setQuoteContent(requestPacket.getQuoteContent());

            java.util.Set<String> memberIds = SessionUtil.getGroupMembers(groupId);
            for (String memberId : memberIds) {
                if (memberId.equals(session.getUserId())) {
                    continue;
                }
                io.netty.channel.group.ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                if (channelGroup != null && !channelGroup.isEmpty()) {
                    channelGroup.writeAndFlush(responsePacket);
                }
            }
        }
    }
}
