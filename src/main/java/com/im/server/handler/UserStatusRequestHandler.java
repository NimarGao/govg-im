package com.im.server.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.UserStatusRequestPacket;
import com.im.protocol.command.UserStatusResponsePacket;
import com.im.server.service.UserService;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@ChannelHandler.Sharable
public class UserStatusRequestHandler extends SimpleChannelInboundHandler<UserStatusRequestPacket> {

    public static final UserStatusRequestHandler INSTANCE = new UserStatusRequestHandler();

    private UserStatusRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UserStatusRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String userId = packet.getUserId();
        String statusType = packet.getStatusType();
        String statusText = packet.getStatusText();

        System.out.println("User Status Update Request: User [" + userId + "] changing state to [" + statusType + "] with message [" + statusText + "]");

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        UserService userService = SpringContextHolder.getBean(UserService.class);

        if (redisTemplate != null) {
            // Save status in Redis Hash
            JSONObject statusObj = new JSONObject();
            statusObj.put("statusType", statusType);
            statusObj.put("statusText", statusText);
            redisTemplate.opsForHash().put("im:user:custom_status", userId, statusObj.toJSONString());
        }

        // Send confirmation ACK/Response back to caller
        UserStatusResponsePacket selfResponse = new UserStatusResponsePacket(userId, statusType, statusText, true);
        ctx.channel().writeAndFlush(selfResponse);

        // Fetch all friends
        if (userService != null) {
            List<String> friends = userService.getFriends(userId);
            if (friends != null && !friends.isEmpty() && redisTemplate != null) {
                for (String friendId : friends) {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("command", Command.USER_STATUS_RESPONSE);
                        payload.put("toUserId", friendId);
                        payload.put("userId", userId);
                        payload.put("statusType", statusType);
                        payload.put("statusText", statusText);
                        payload.put("online", true);

                        // Broadcast to cluster
                        redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast status update to friend [" + friendId + "]: " + e.getMessage());
                    }
                }
            } else if (friends != null && !friends.isEmpty()) {
                // Standalone Fallback
                for (String friendId : friends) {
                    ChannelGroup friendGroup = SessionUtil.getChannelGroup(friendId);
                    if (friendGroup != null && !friendGroup.isEmpty()) {
                        friendGroup.writeAndFlush(selfResponse);
                    }
                }
            }
        }
    }
}
