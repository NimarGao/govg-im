package com.im.server.handler;

import com.im.util.Session;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ConnectionHandler extends ChannelInboundHandlerAdapter {

    public static final ConnectionHandler INSTANCE = new ConnectionHandler();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (SessionUtil.hasLogin(ctx.channel())) {
            com.im.util.Session session = SessionUtil.getSession(ctx.channel());
            System.out.println("User [" + session.getUserName() + "] disconnected.");
            
            SessionUtil.unbindSession(ctx.channel());
            
            // If all devices of the user disconnected, remove the Redis session
            if (SessionUtil.getChannelGroup(session.getUserId()) == null) {
                try {
                    com.im.util.redis.RedisService redisService = com.im.util.SpringContextHolder.getBean(com.im.util.redis.RedisService.class);
                    redisService.removeSession(session.getUserId());
                    System.out.println("Global Redis Session cleared for User: " + session.getUserId());
                } catch (Exception e) {
                    System.err.println("Error removing global Redis session: " + e.getMessage());
                }
            }
        }
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
