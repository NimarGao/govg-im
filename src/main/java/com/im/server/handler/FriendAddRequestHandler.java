package com.im.server.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.FriendAddRequestPacket;
import com.im.protocol.command.FriendAddResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

@ChannelHandler.Sharable
public class FriendAddRequestHandler extends SimpleChannelInboundHandler<FriendAddRequestPacket> {

    public static final FriendAddRequestHandler INSTANCE = new FriendAddRequestHandler();

    private FriendAddRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FriendAddRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String currentUserId = session.getUserId();
        String targetUserId = packet.getTargetUserId();
        String remark = packet.getRemark();
        String status = packet.getStatus(); // "pending" | "accepted" | "rejected"

        System.out.println("FriendAdd Request: User [" + currentUserId + "], Target [" + targetUserId + "], Status [" + status + "]");

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        if (redisTemplate == null) {
            System.err.println("RedisTemplate not found, friend add request handler aborted.");
            return;
        }

        String requestsKey = "im:user:friend_requests:" + targetUserId;
        String ownRequestsKey = "im:user:friend_requests:" + currentUserId;

        if ("pending".equalsIgnoreCase(status)) {
            // Check if they are already blacklisted by target user before adding friend
            Boolean isBlacklisted = redisTemplate.opsForSet().isMember("im:user:blacklist:" + targetUserId, currentUserId);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                System.out.println("FriendAdd blocked: User [" + currentUserId + "] is blacklisted by [" + targetUserId + "]");
                return;
            }

            // A sends request to B. B's requests key receives: key = A, value = request details
            JSONObject requestObj = new JSONObject();
            requestObj.put("userId", currentUserId);
            requestObj.put("targetUserId", targetUserId);
            requestObj.put("remark", remark);
            requestObj.put("status", "pending");
            requestObj.put("createTime", System.currentTimeMillis());

            redisTemplate.opsForHash().put(requestsKey, currentUserId, requestObj.toJSONString());
            System.out.println("Saved friend request from [" + currentUserId + "] to [" + targetUserId + "]");

        } else if ("accepted".equalsIgnoreCase(status)) {
            // Remove request from B's pending request map
            redisTemplate.opsForHash().delete(ownRequestsKey, targetUserId);

            // Add each other to friend list
            redisTemplate.opsForSet().add("im:user:friends:" + currentUserId, targetUserId);
            redisTemplate.opsForSet().add("im:user:friends:" + targetUserId, currentUserId);

            // If B had A in B's blacklist, remove A from B's blacklist
            redisTemplate.opsForSet().remove("im:user:blacklist:" + currentUserId, targetUserId);
            redisTemplate.opsForSet().remove("im:user:blacklist:" + targetUserId, currentUserId);

            System.out.println("Friend request accepted: [" + currentUserId + "] and [" + targetUserId + "] are now friends.");

        } else if ("rejected".equalsIgnoreCase(status)) {
            // Remove request from B's pending request map
            redisTemplate.opsForHash().delete(ownRequestsKey, targetUserId);
            System.out.println("Friend request rejected: [" + currentUserId + "] rejected [" + targetUserId + "]");
        }

        // Fetch full pending lists
        String targetListJson = getPendingRequestsJson(redisTemplate, targetUserId);
        String currentListJson = getPendingRequestsJson(redisTemplate, currentUserId);

        // Broadcast updates
        broadcastFriendAdd(redisTemplate, targetUserId, currentUserId, remark, status, targetListJson);
        broadcastFriendAdd(redisTemplate, currentUserId, targetUserId, remark, status, currentListJson);
    }

    private String getPendingRequestsJson(StringRedisTemplate redisTemplate, String userId) {
        String key = "im:user:friend_requests:" + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        JSONArray arr = new JSONArray();
        for (Object val : entries.values()) {
            arr.add(JSON.parseObject((String) val));
        }
        return arr.toJSONString();
    }

    private void broadcastFriendAdd(StringRedisTemplate redisTemplate, String toUserId, String targetUserId, String remark, String status, String requestsListJson) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("command", Command.FRIEND_ADD_RESPONSE);
            payload.put("userId", toUserId);
            payload.put("targetUserId", targetUserId);
            payload.put("remark", remark);
            payload.put("status", status);
            payload.put("requestsListJson", requestsListJson);

            redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
            System.out.println("Distributed FriendAdd Broadcast: Sent to ROUTE_CHANNEL for User [" + toUserId + "]");
        } catch (Exception e) {
            System.err.println("Redis cluster friend add broadcast failed. Falling back to local:");
            e.printStackTrace();

            // Standalone Fallback
            ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                FriendAddResponsePacket responsePacket = new FriendAddResponsePacket(
                    toUserId, targetUserId, remark, status, requestsListJson
                );
                channelGroup.writeAndFlush(responsePacket);
            }
        }
    }
}
