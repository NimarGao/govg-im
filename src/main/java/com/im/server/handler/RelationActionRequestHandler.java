package com.im.server.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.RelationActionRequestPacket;
import com.im.protocol.command.RelationActionResponsePacket;
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
public class RelationActionRequestHandler extends SimpleChannelInboundHandler<RelationActionRequestPacket> {

    public static final RelationActionRequestHandler INSTANCE = new RelationActionRequestHandler();

    private RelationActionRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RelationActionRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String currentUserId = session.getUserId();
        String targetUserId = packet.getTargetUserId();
        String action = packet.getAction(); // "add_blacklist" | "remove_blacklist" | "delete_friend"

        System.out.println("RelationAction Request: User [" + currentUserId + "], Target [" + targetUserId + "], Action [" + action + "]");

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        if (redisTemplate == null) {
            System.err.println("RedisTemplate not found, relation action request handler aborted.");
            return;
        }

        boolean success = false;

        if ("add_blacklist".equalsIgnoreCase(action)) {
            // Add to blacklist
            redisTemplate.opsForSet().add("im:user:blacklist:" + currentUserId, targetUserId);
            // Remove friendship
            redisTemplate.opsForSet().remove("im:user:friends:" + currentUserId, targetUserId);
            redisTemplate.opsForSet().remove("im:user:friends:" + targetUserId, currentUserId);
            success = true;
            System.out.println("User [" + currentUserId + "] blacklisted [" + targetUserId + "]");

        } else if ("remove_blacklist".equalsIgnoreCase(action)) {
            // Remove from blacklist
            redisTemplate.opsForSet().remove("im:user:blacklist:" + currentUserId, targetUserId);
            success = true;
            System.out.println("User [" + currentUserId + "] unblacklisted [" + targetUserId + "]");

        } else if ("delete_friend".equalsIgnoreCase(action)) {
            // Remove friendship
            redisTemplate.opsForSet().remove("im:user:friends:" + currentUserId, targetUserId);
            redisTemplate.opsForSet().remove("im:user:friends:" + targetUserId, currentUserId);
            success = true;
            System.out.println("User [" + currentUserId + "] deleted friend [" + targetUserId + "]");
        }

        // Get latest blacklists
        String blacklistJson = getBlacklistJson(redisTemplate, currentUserId);
        String targetBlacklistJson = getBlacklistJson(redisTemplate, targetUserId);

        // Broadcast current user update
        broadcastRelationAction(redisTemplate, currentUserId, targetUserId, action, success, blacklistJson);
        // Broadcast target user update
        broadcastRelationAction(redisTemplate, targetUserId, currentUserId, action, success, targetBlacklistJson);
    }

    private String getBlacklistJson(StringRedisTemplate redisTemplate, String userId) {
        Set<String> blacklist = redisTemplate.opsForSet().members("im:user:blacklist:" + userId);
        JSONArray arr = new JSONArray();
        if (blacklist != null) {
            for (String item : blacklist) {
                arr.add(item);
            }
        }
        return arr.toJSONString();
    }

    private void broadcastRelationAction(StringRedisTemplate redisTemplate, String toUserId, String targetUserId, String action, boolean success, String blacklistJson) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("command", Command.RELATION_ACTION_RESPONSE);
            payload.put("userId", toUserId);
            payload.put("targetUserId", targetUserId);
            payload.put("action", action);
            payload.put("success", success);
            payload.put("blacklistJson", blacklistJson);

            redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
            System.out.println("Distributed RelationAction Broadcast: Sent to ROUTE_CHANNEL for User [" + toUserId + "]");
        } catch (Exception e) {
            System.err.println("Redis cluster relation action broadcast failed. Falling back to local:");
            e.printStackTrace();

            // Standalone Fallback
            ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                RelationActionResponsePacket responsePacket = new RelationActionResponsePacket(
                    action, toUserId, targetUserId, success, blacklistJson
                );
                channelGroup.writeAndFlush(responsePacket);
            }
        }
    }
}
