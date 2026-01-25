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

        JoinGroupResponsePacket responsePacket = new JoinGroupResponsePacket();
        responsePacket.setGroupId(groupId);
        responsePacket.setSuccess(true);
        
        ctx.channel().writeAndFlush(responsePacket);
        System.out.println("User " + userId + " joined group " + groupId);
    }
}
