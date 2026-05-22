package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.TypingRequestPacket;
import com.im.protocol.command.TypingResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import com.im.util.redis.RedisService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

@ChannelHandler.Sharable
public class TypingRequestHandler extends SimpleChannelInboundHandler<TypingRequestPacket> {

    public static final TypingRequestHandler INSTANCE = new TypingRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TypingRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }
        String fromUserId = session.getUserId();
        String toUserId = packet.getToUserId();

        System.out.println("Typing Signal: User [" + fromUserId + "] is typing in session with User [" + toUserId + "]");

        TypingResponsePacket responsePacket = new TypingResponsePacket(fromUserId);

        try {
            RedisService redisService = SpringContextHolder.getBean(RedisService.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            String globalSession = redisService != null ? redisService.getSession(toUserId) : null;
            if (globalSession != null) {
                if (redisTemplate != null) {
                    JSONObject payload = new JSONObject();
                    payload.put("command", Command.TYPING_RESPONSE);
                    payload.put("toUserId", toUserId);
                    payload.put("fromUserId", fromUserId);

                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                    System.out.println("Distributed Typing Match: Route payload sent for User [" + toUserId + "]");
                }
            } else {
                System.out.println("Target user [" + toUserId + "] is globally offline. Ignoring typing status.");
            }
        } catch (Exception e) {
            System.err.println("Redis cluster services failed. Falling back to simple standalone local delivery for typing signal:");
            e.printStackTrace();

            ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                channelGroup.writeAndFlush(responsePacket);
                System.out.println("Standalone Fallback: Typing signal dispatched locally to User [" + toUserId + "]");
            }
        }
    }
}
