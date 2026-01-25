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
            Session session = SessionUtil.getSession(ctx.channel());
            System.out.println("User [" + session.getUserName() + "] disconnected.");
            
            // Notify friends logic here
            // UserService.notifyFriends(session.getUserId(), "OFFLINE");
            
            SessionUtil.unbindSession(ctx.channel());
        }
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
