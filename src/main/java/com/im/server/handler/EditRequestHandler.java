package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.entity.Message;
import com.im.mapper.MessageMapper;
import com.im.protocol.command.Command;
import com.im.protocol.command.EditRequestPacket;
import com.im.protocol.command.EditResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Set;

@ChannelHandler.Sharable
public class EditRequestHandler extends SimpleChannelInboundHandler<EditRequestPacket> {

    public static final EditRequestHandler INSTANCE = new EditRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, EditRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String fromUserId = session.getUserId();
        String msgId = packet.getMsgId();
        String toUserId = packet.getToUserId();
        String toGroupId = packet.getToGroupId();
        String newContent = packet.getNewContent();

        System.out.println("Edit Request: User [" + fromUserId + "] requests to edit message [" + msgId + "]");

        EditResponsePacket responsePacket = new EditResponsePacket();
        responsePacket.setMsgId(msgId);
        responsePacket.setToUserId(toUserId);
        responsePacket.setToGroupId(toGroupId);
        responsePacket.setFromUserId(fromUserId);
        responsePacket.setNewContent(newContent);

        MessageMapper messageMapper = SpringContextHolder.getBean(MessageMapper.class);
        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

        if (messageMapper == null) {
            responsePacket.setSuccess(false);
            responsePacket.setErrorMsg("Internal database error: MessageMapper not found");
            ctx.channel().writeAndFlush(responsePacket);
            return;
        }

        // 1. Query message from MySQL
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("msg_id", msgId);
        Message dbMessage = messageMapper.selectOne(queryWrapper);

        if (dbMessage == null) {
            responsePacket.setSuccess(false);
            responsePacket.setErrorMsg("Message does not exist in database");
            ctx.channel().writeAndFlush(responsePacket);
            return;
        }

        // 2. Validate Sender
        if (!fromUserId.equals(dbMessage.getFromUserId())) {
            responsePacket.setSuccess(false);
            responsePacket.setErrorMsg("You can only edit your own messages");
            ctx.channel().writeAndFlush(responsePacket);
            return;
        }

        // 3. Update message content in DB
        dbMessage.setContent(newContent);
        // We can also flag it as edited, though content update is the primary goal. 
        // Some systems might have an extra status or column, but in our case, updating content is perfect.
        messageMapper.updateById(dbMessage);
        System.out.println("Persistence: Message [" + msgId + "] content updated to: " + newContent + " in MySQL");

        // 4. Broadcast Edit Response
        responsePacket.setSuccess(true);
        ctx.channel().writeAndFlush(responsePacket);

        // Build broadcast payload
        JSONObject payload = new JSONObject();
        payload.put("command", Command.EDIT_RESPONSE);
        payload.put("msgId", msgId);
        payload.put("toUserId", toUserId);
        payload.put("toGroupId", toGroupId);
        payload.put("fromUserId", fromUserId);
        payload.put("newContent", newContent);
        payload.put("success", true);

        try {
            if (redisTemplate != null) {
                if (toGroupId != null && !toGroupId.isEmpty()) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_GROUP_CHANNEL, payload.toJSONString());
                } else if (toUserId != null && !toUserId.isEmpty()) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                }
                System.out.println("Distributed Edit: Published edit event via Redis Pub/Sub");
            }
        } catch (Exception e) {
            System.err.println("Redis Pub/Sub broadcast failed. Falling back to local delivery:");
            e.printStackTrace();

            if (toGroupId != null && !toGroupId.isEmpty()) {
                Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
                for (String memberId : memberIds) {
                    if (memberId.equals(fromUserId)) {
                        continue;
                    }
                    ChannelGroup cg = SessionUtil.getChannelGroup(memberId);
                    if (cg != null && !cg.isEmpty()) {
                        cg.writeAndFlush(responsePacket);
                    }
                }
            } else if (toUserId != null && !toUserId.isEmpty()) {
                ChannelGroup cg = SessionUtil.getChannelGroup(toUserId);
                if (cg != null && !cg.isEmpty()) {
                    cg.writeAndFlush(responsePacket);
                }
            }
        }
    }
}
