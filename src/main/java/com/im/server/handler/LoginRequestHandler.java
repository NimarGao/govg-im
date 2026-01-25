package com.im.server.handler;

import com.im.protocol.command.LoginRequestPacket;
import com.im.protocol.command.LoginResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

@ChannelHandler.Sharable
public class LoginRequestHandler extends SimpleChannelInboundHandler<LoginRequestPacket> {

    public static final LoginRequestHandler INSTANCE = new LoginRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestPacket loginRequestPacket) {
        LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
        loginResponsePacket.setVersion(loginRequestPacket.getVersion());
        loginResponsePacket.setUsername(loginRequestPacket.getUsername());

        if (isValid(loginRequestPacket)) {
            loginResponsePacket.setSuccess(true);
            String userId = UUID.nameUUIDFromBytes(loginRequestPacket.getUsername().getBytes()).toString();
            loginResponsePacket.setUserId(userId);
            SessionUtil.bindSession(new Session(userId, loginRequestPacket.getUsername()), ctx.channel());
            System.out.println("User [" + loginRequestPacket.getUsername() + "] logged in successfully");
        } else {
            loginResponsePacket.setReason("Invalid credentials");
            loginResponsePacket.setSuccess(false);
            System.out.println("User [" + loginRequestPacket.getUsername() + "] failed to log in");
        }

        // Write response
        ctx.channel().writeAndFlush(loginResponsePacket);
    }

    private boolean isValid(LoginRequestPacket loginRequestPacket) {
        return true; // Always valid for demo
    }
}
