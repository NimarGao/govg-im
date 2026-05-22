package com.im.server.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.MultiVoiceCallRequestPacket;
import com.im.protocol.command.MultiVoiceCallResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class MultiVoiceCallRequestHandler extends SimpleChannelInboundHandler<MultiVoiceCallRequestPacket> {

    public static final MultiVoiceCallRequestHandler INSTANCE = new MultiVoiceCallRequestHandler();

    private MultiVoiceCallRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MultiVoiceCallRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String groupId = packet.getGroupId();
        String action = packet.getAction();
        String userId = packet.getUserId();
        Boolean mute = packet.getMute() != null ? packet.getMute() : false;

        System.out.println("MultiVoiceCall Request: Group [" + groupId + "], User [" + userId + "], Action [" + action + "], Mute [" + mute + "]");

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        if (redisTemplate == null) {
            System.err.println("RedisTemplate not found, multi-voice call handler aborted.");
            return;
        }

        String redisKey = "im:group:call:" + groupId;

        // Perform Redis state updates based on action
        if ("start".equalsIgnoreCase(action)) {
            // Initiate meeting room, put caller as the first member
            redisTemplate.opsForHash().put(redisKey, userId, String.valueOf(mute));
            redisTemplate.expire(redisKey, 2, TimeUnit.HOURS);
        } else if ("join".equalsIgnoreCase(action)) {
            redisTemplate.opsForHash().put(redisKey, userId, String.valueOf(mute));
        } else if ("toggle_mute".equalsIgnoreCase(action)) {
            redisTemplate.opsForHash().put(redisKey, userId, String.valueOf(mute));
        } else if ("leave".equalsIgnoreCase(action)) {
            redisTemplate.opsForHash().delete(redisKey, userId);
            // If no members are left in the call, destroy it
            Long size = redisTemplate.opsForHash().size(redisKey);
            if (size == null || size == 0) {
                redisTemplate.delete(redisKey);
            }
        }

        // Get latest active member list
        Map<Object, Object> activeMembersMap = redisTemplate.opsForHash().entries(redisKey);
        JSONArray membersArray = new JSONArray();
        for (Map.Entry<Object, Object> entry : activeMembersMap.entrySet()) {
            JSONObject memberObj = new JSONObject();
            memberObj.put("userId", String.valueOf(entry.getKey()));
            memberObj.put("mute", Boolean.parseBoolean(String.valueOf(entry.getValue())));
            membersArray.add(memberObj);
        }
        String membersJson = membersArray.toJSONString();

        System.out.println("MultiVoiceCall Meeting State for Group [" + groupId + "]: " + membersJson);

        // Broadcast meeting update to all group members via Redis PubSub
        try {
            JSONObject payload = new JSONObject();
            payload.put("command", Command.MULTI_VOICE_CALL_RESPONSE);
            payload.put("groupId", groupId);
            payload.put("action", action);
            payload.put("userId", userId);
            payload.put("mute", mute);
            payload.put("members", membersJson);

            redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
            System.out.println("Distributed MultiVoiceCall Broadcast: Sent to ROUTE_CHANNEL for Group [" + groupId + "]");
            return;
        } catch (Exception e) {
            System.err.println("Redis cluster multi-voice call broadcast failed. Falling back to standalone:");
            e.printStackTrace();
        }

        // Standalone Fallback: push to all local group members
        Set<String> memberIds = SessionUtil.getGroupMembers(groupId);
        if (memberIds != null) {
            MultiVoiceCallResponsePacket responsePacket = new MultiVoiceCallResponsePacket(
                groupId, action, userId, mute, membersJson
            );
            for (String memberId : memberIds) {
                ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                if (channelGroup != null && !channelGroup.isEmpty()) {
                    channelGroup.writeAndFlush(responsePacket);
                }
            }
        }
    }
}
