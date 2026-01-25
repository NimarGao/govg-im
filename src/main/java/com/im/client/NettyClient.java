package com.im.client;

import com.im.protocol.command.*;
import com.im.protocol.codec.IMPacketDecoder;
import com.im.protocol.codec.IMPacketEncoder;
import com.im.protocol.codec.Spliter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

public class NettyClient {

    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(new IMPacketDecoder());
                        ch.pipeline().addLast(new LoginResponseHandler());
                        ch.pipeline().addLast(new MessageResponseHandler());
                        ch.pipeline().addLast(new CreateGroupResponseHandler());
                        ch.pipeline().addLast(new JoinGroupResponseHandler());
                        ch.pipeline().addLast(new GroupMessageResponseHandler());
                        ch.pipeline().addLast(new AckHandler());
                        ch.pipeline().addLast(new IMPacketEncoder());
                    }
                });

        connect(bootstrap, "1.94.115.162", 10086);
    }

    private static void connect(Bootstrap bootstrap, String host, int port) {
        bootstrap.connect(host, port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Connected to server!");
                Channel channel = ((ChannelFuture) future).channel();
                startConsole(channel);
            } else {
                System.err.println("Connection failed!");
            }
        });
    }

    private static void startConsole(Channel channel) {
        Scanner scanner = new Scanner(System.in);
        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (!SessionUtil.hasLogin(channel)) {
                    System.out.println("Enter username to login:");
                    String username = scanner.nextLine();
                    LoginRequestPacket loginRequestPacket = new LoginRequestPacket();
                    loginRequestPacket.setUsername(username);
                    loginRequestPacket.setPassword("pwd");
                    channel.writeAndFlush(loginRequestPacket);
                    waitForLogin();
                } else {
                    System.out.println("Commands: [userId:msg] [group:groupId:msg] [create:u1,u2] [join:groupId]");
                    String line = scanner.nextLine();
                    String[] split = line.split(":");
                    
                    try {
                        if (line.startsWith("create:")) {
                            String[] userIds = split[1].split(",");
                            CreateGroupRequestPacket packet = new CreateGroupRequestPacket();
                            packet.setUserIdList(Arrays.asList(userIds));
                            channel.writeAndFlush(packet);
                        } else if (line.startsWith("join:")) {
                            String groupId = split[1];
                            JoinGroupRequestPacket packet = new JoinGroupRequestPacket();
                            packet.setGroupId(groupId);
                            channel.writeAndFlush(packet);
                        } else if (line.startsWith("group:")) {
                            String groupId = split[1];
                            String msg = split[2];
                            GroupMessageRequestPacket packet = new GroupMessageRequestPacket(groupId, msg);
                            packet.setMsgId(UUID.randomUUID().toString());
                            channel.writeAndFlush(packet);
                        } else if (split.length == 2) {
                            MessageRequestPacket packet = new MessageRequestPacket(split[0], split[1]);
                            packet.setMsgId(UUID.randomUUID().toString());
                            channel.writeAndFlush(packet);
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid command format.");
                    }
                }
            }
        }).start();
    }

    private static void waitForLogin() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class LoginResponseHandler extends SimpleChannelInboundHandler<LoginResponsePacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, LoginResponsePacket packet) {
            if (packet.isSuccess()) {
                System.out.println("Login successful! UserId: " + packet.getUserId());
                SessionUtil.markAsLogin(ctx.channel());
            } else {
                System.out.println("Login failed: " + packet.getReason());
            }
        }
    }

    public static class MessageResponseHandler extends SimpleChannelInboundHandler<MessageResponsePacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MessageResponsePacket packet) {
            System.out.println("Received message from " + packet.getFromUserName() + ": " + packet.getMessage());
            // Send ACK
            if (packet.getMsgId() != null) {
                ctx.channel().writeAndFlush(new AckPacket(packet.getMsgId(), "CLIENT"));
            }
        }
    }

    public static class CreateGroupResponseHandler extends SimpleChannelInboundHandler<CreateGroupResponsePacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CreateGroupResponsePacket packet) {
            System.out.println("Group created! ID: " + packet.getGroupId() + ", Members: " + packet.getUserNameList());
        }
    }

    public static class JoinGroupResponseHandler extends SimpleChannelInboundHandler<JoinGroupResponsePacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, JoinGroupResponsePacket packet) {
            if (packet.isSuccess()) {
                System.out.println("Joined group: " + packet.getGroupId());
            } else {
                System.out.println("Failed to join group: " + packet.getReason());
            }
        }
    }

    public static class GroupMessageResponseHandler extends SimpleChannelInboundHandler<GroupMessageResponsePacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, GroupMessageResponsePacket packet) {
            System.out.println("Received Group[" + packet.getFromGroupId() + "] message from " + packet.getFromUser() + ": " + packet.getMessage());
            // Send ACK logic...
        }
    }
    
    public static class AckHandler extends SimpleChannelInboundHandler<AckPacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, AckPacket packet) {
            System.out.println("Server ACK received for msg: " + packet.getMsgId());
        }
    }

    public static class SessionUtil {
        private static boolean hasLogin = false;
        public static boolean hasLogin(Channel channel) { return hasLogin; }
        public static void markAsLogin(Channel channel) { hasLogin = true; }
    }
}
