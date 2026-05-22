package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.GroupReadReceiptRequestPacket;
import com.im.protocol.command.GroupReadReceiptResponsePacket;
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
public class GroupReadReceiptRequestHandler extends SimpleChannelInboundHandler<GroupReadReceiptRequestPacket> {

    public static final GroupReadReceiptRequestHandler INSTANCE = new GroupReadReceiptRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupReadReceiptRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String readerUserId = session.getUserId();
        String groupId = packet.getGroupId();
        String msgId = packet.getMsgId();

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        if (redisTemplate == null) {
            return;
        }

        System.out.println("Group Read Receipt: User [" + readerUserId + "] read message [" + msgId + "] in Group [" + groupId + "]");

        // 1. Add reader to Redis Set for this message
        String redisKey = "im:group:read:" + msgId;
        redisTemplate.opsForSet().add(redisKey, readerUserId);

        // 2. Retrieve all readers
        Set<String> readUserIds = redisTemplate.opsForSet().members(redisKey);
        int readCount = readUserIds != null ? readUserIds.size() : 0;

        // 3. Construct response packet
        GroupReadReceiptResponsePacket responsePacket = new GroupReadReceiptResponsePacket(groupId, msgId, readCount, readUserIds);

        // 4. Construct broadcast JSON
        JSONObject payload = new JSONObject();
        payload.put("command", Command.GROUP_READ_RECEIPT_RESPONSE);
        payload.put("groupId", groupId);
        payload.put("msgId", msgId);
        payload.put("readCount", readCount);
        payload.put("readUserIds", readUserIds);

        // 5. Broadcast to all members using Redis Pub/Sub
        try {
            redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_GROUP_CHANNEL, payload.toJSONString());
            System.out.println("Distributed Group Read: Published group read receipt to Redis for message [" + msgId + "]");
        } catch (Exception e) {
            System.err.println("Redis Pub/Sub failed for group read receipt. Falling back to local delivery:");
            e.printStackTrace();

            // Local fallback
            Set<String> memberIds = SessionUtil.getGroupMembers(groupId);
            if (memberIds != null) {
                for (String memberId : memberIds) {
                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        channelGroup.writeAndFlush(responsePacket);
                    }
                }
            }
        }
    }
}
