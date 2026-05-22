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

        // 2. Construct Broadcast Packet payload in JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("toGroupId", groupId);
        jsonObject.put("message", requestPacket.getMessage());
        jsonObject.put("fromUser", session.getUserId());
        jsonObject.put("msgId", requestPacket.getMsgId());
        jsonObject.put("msgType", requestPacket.getMsgType());

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
