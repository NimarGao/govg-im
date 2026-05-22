package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.entity.Message;
import com.im.mapper.MessageMapper;
import com.im.protocol.command.Command;
import com.im.protocol.command.RecallRequestPacket;
import com.im.protocol.command.RecallResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Date;
import java.util.Set;

@ChannelHandler.Sharable
public class RecallRequestHandler extends SimpleChannelInboundHandler<RecallRequestPacket> {

    public static final RecallRequestHandler INSTANCE = new RecallRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RecallRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String fromUserId = session.getUserId();
        String msgId = packet.getMsgId();
        String toUserId = packet.getToUserId();
        String toGroupId = packet.getToGroupId();

        System.out.println("Recall Request: User [" + fromUserId + "] requests to recall message [" + msgId + "]");

        RecallResponsePacket responsePacket = new RecallResponsePacket();
        responsePacket.setMsgId(msgId);
        responsePacket.setToUserId(toUserId);
        responsePacket.setToGroupId(toGroupId);
        responsePacket.setFromUserId(fromUserId);

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
            responsePacket.setErrorMsg("You can only recall your own messages");
            ctx.channel().writeAndFlush(responsePacket);
            return;
        }

        // 3. Validate 2 Minutes Time Limit
        long now = System.currentTimeMillis();
        long createTime = dbMessage.getCreateTime().getTime();
        if (now - createTime > 120_000) {
            responsePacket.setSuccess(false);
            responsePacket.setErrorMsg("Messages sent over 2 minutes ago cannot be recalled");
            ctx.channel().writeAndFlush(responsePacket);
            return;
        }

        // 4. Update message in DB (Status = 3 represents Recalled, and replace content)
        dbMessage.setStatus(3);
        dbMessage.setContent("[撤回消息]");
        messageMapper.updateById(dbMessage);
        System.out.println("Persistence: Message [" + msgId + "] status set to 3 (Recalled) in MySQL");

        // 5. Broadcast Recall Response
        responsePacket.setSuccess(true);
        ctx.channel().writeAndFlush(responsePacket);

        // Build broadcast payload
        JSONObject payload = new JSONObject();
        payload.put("command", Command.RECALL_RESPONSE);
        payload.put("msgId", msgId);
        payload.put("toUserId", toUserId);
        payload.put("toGroupId", toGroupId);
        payload.put("fromUserId", fromUserId);
        payload.put("success", true);

        try {
            if (redisTemplate != null) {
                if (toGroupId != null && !toGroupId.isEmpty()) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_GROUP_CHANNEL, payload.toJSONString());
                } else if (toUserId != null && !toUserId.isEmpty()) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                }
                System.out.println("Distributed Recall: Published recall event via Redis Pub/Sub");
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
