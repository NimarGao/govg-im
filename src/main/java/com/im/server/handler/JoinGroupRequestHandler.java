package com.im.server.handler;

import com.im.protocol.command.JoinGroupRequestPacket;
import com.im.protocol.command.JoinGroupResponsePacket;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class JoinGroupRequestHandler extends SimpleChannelInboundHandler<JoinGroupRequestPacket> {

    public static final JoinGroupRequestHandler INSTANCE = new JoinGroupRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JoinGroupRequestPacket requestPacket) {
        String groupId = requestPacket.getGroupId();
        String userId = SessionUtil.getSession(ctx.channel()).getUserId();

        SessionUtil.bindChannelGroup(groupId, userId);

        try {
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate = com.im.util.SpringContextHolder.getBean(org.springframework.data.redis.core.StringRedisTemplate.class);
            if (redisTemplate != null) {
                String groupKey = "im:group:members:" + groupId;
                redisTemplate.opsForSet().add(groupKey, userId);
                System.out.println("Redis Join Group Mapping Saved: User [" + userId + "] added to Group [" + groupId + "] globally.");
            }
        } catch (Exception ex) {
            System.err.println("Failed to write joining group member to Redis Set: " + ex.getMessage());
        }

        JoinGroupResponsePacket responsePacket = new JoinGroupResponsePacket();
        responsePacket.setGroupId(groupId);
        responsePacket.setSuccess(true);
        
        ctx.channel().writeAndFlush(responsePacket);
        System.out.println("User " + userId + " joined group " + groupId);
    }
}
