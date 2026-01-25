package com.im.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.im.protocol.Packet;
import com.im.protocol.command.*;
import com.im.server.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

@Component
public class NettyWebSocketServer {

    @Value("${im.server.websocket-port}")
    private int port;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new HttpObjectAggregator(8192));
                        ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                        ch.pipeline().addLast(new WebSocketPacketEncoder());
                        ch.pipeline().addLast(new WebSocketFrameHandler());
                        ch.pipeline().addLast(ConnectionHandler.INSTANCE);
                        ch.pipeline().addLast(LoginRequestHandler.INSTANCE);
                        ch.pipeline().addLast(AuthHandler.INSTANCE);
                        ch.pipeline().addLast(MessageRequestHandler.INSTANCE);
                        ch.pipeline().addLast(CreateGroupRequestHandler.INSTANCE);
                        ch.pipeline().addLast(JoinGroupRequestHandler.INSTANCE);
                        ch.pipeline().addLast(GroupMessageRequestHandler.INSTANCE);
                        ch.pipeline().addLast(AckHandler.INSTANCE);
                    }
                });

        serverBootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Netty WebSocket Server started on port: " + port);
            }
        });
    }

    @PreDestroy
    public void destroy() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    public static class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
            String text = frame.text();
            JSONObject json = JSON.parseObject(text);
            Byte command = json.getByte("command");

            Packet packet = null;
            if (Command.LOGIN_REQUEST.equals(command)) {
                packet = json.toJavaObject(LoginRequestPacket.class);
            } else if (Command.MESSAGE_REQUEST.equals(command)) {
                packet = json.toJavaObject(MessageRequestPacket.class);
            } else if (Command.CREATE_GROUP_REQUEST.equals(command)) {
                packet = json.toJavaObject(CreateGroupRequestPacket.class);
            } else if (Command.JOIN_GROUP_REQUEST.equals(command)) {
                packet = json.toJavaObject(JoinGroupRequestPacket.class);
            } else if (Command.GROUP_MESSAGE_REQUEST.equals(command)) {
                packet = json.toJavaObject(GroupMessageRequestPacket.class);
            } else if (Command.ACK_PACKET.equals(command)) {
                packet = json.toJavaObject(AckPacket.class);
            }

            if (packet != null) {
                ctx.fireChannelRead(packet);
            }
        }
    }

    public static class WebSocketPacketEncoder extends MessageToMessageEncoder<Packet> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Packet packet, List<Object> out) throws Exception {
            String json = JSON.toJSONString(packet);
            out.add(new TextWebSocketFrame(json));
        }
    }
}
