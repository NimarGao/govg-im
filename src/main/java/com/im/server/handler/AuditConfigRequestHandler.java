package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.AuditConfigRequestPacket;
import com.im.protocol.command.AuditConfigResponsePacket;
import com.im.util.SensitiveWordFilter;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.data.redis.core.StringRedisTemplate;

@ChannelHandler.Sharable
public class AuditConfigRequestHandler extends SimpleChannelInboundHandler<AuditConfigRequestPacket> {

    public static final AuditConfigRequestHandler INSTANCE = new AuditConfigRequestHandler();

    private AuditConfigRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AuditConfigRequestPacket packet) throws Exception {
        String policy = packet.getPolicy();
        java.util.List<String> words = packet.getSensitiveWords();

        System.out.println("Audit Configuration Update Request: Setting policy to [" + policy + "] and words count to [" + (words != null ? words.size() : 0) + "]");

        // 1. Update local filter instance
        SensitiveWordFilter.INSTANCE.setPolicy(policy);
        SensitiveWordFilter.INSTANCE.setSensitiveWords(words);

        StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

        // 2. Broadcast configuration sync to Redis Pub/Sub cluster
        if (redisTemplate != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("command", Command.AUDIT_CONFIG_RESPONSE);
                payload.put("syncAudit", true);
                payload.put("policy", policy);
                payload.put("sensitiveWords", words);

                redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, payload.toJSONString());
                System.out.println("Distributed Signaling: Route payload sent for content auditing config sync");
            } catch (Exception e) {
                System.err.println("Redis cluster config sync failed: " + e.getMessage());
            }
        }

        // 3. Send response back to the client initiating the request
        AuditConfigResponsePacket response = new AuditConfigResponsePacket(
            true, 
            SensitiveWordFilter.INSTANCE.getPolicy(), 
            SensitiveWordFilter.INSTANCE.getSensitiveWords()
        );
        ctx.channel().writeAndFlush(response);
    }
}
