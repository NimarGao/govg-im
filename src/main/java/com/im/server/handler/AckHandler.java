package com.im.server.handler;

import com.im.protocol.command.AckPacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable
public class AckHandler extends SimpleChannelInboundHandler<AckPacket> {

    public static final AckHandler INSTANCE = new AckHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AckPacket ackPacket) {
        Session session = SessionUtil.getSession(ctx.channel());
        System.out.println("Received ACK for msg " + ackPacket.getMsgId() + " from user " + session.getUserId());
        
        // Update message status in DB to 'DELIVERED'
    }
}
