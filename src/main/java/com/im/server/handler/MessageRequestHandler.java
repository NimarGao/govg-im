package com.im.server.handler;

import com.im.protocol.command.AckPacket;
import com.im.protocol.command.MessageRequestPacket;
import com.im.protocol.command.MessageResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

@ChannelHandler.Sharable
public class MessageRequestHandler extends SimpleChannelInboundHandler<MessageRequestPacket> {

    public static final MessageRequestHandler INSTANCE = new MessageRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageRequestPacket messageRequestPacket) {
        // 1. Get current session
        Session session = SessionUtil.getSession(ctx.channel());

        // 2. Simulating Message Persistence (Save to DB)
        System.out.println("Persistence: Saving message " + messageRequestPacket.getMsgId() + " from " + session.getUserId());

        // 3. Send ACK to Sender
        if (messageRequestPacket.getMsgId() != null) {
            AckPacket ackPacket = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
            ctx.channel().writeAndFlush(ackPacket);
        }

        // 4. Construct response packet
        MessageResponsePacket messageResponsePacket = new MessageResponsePacket();
        messageResponsePacket.setFromUserId(session.getUserId());
        messageResponsePacket.setFromUserName(session.getUserName());
        messageResponsePacket.setMessage(messageRequestPacket.getMessage());
        messageResponsePacket.setMsgId(messageRequestPacket.getMsgId());
        messageResponsePacket.setMsgType(messageRequestPacket.getMsgType());

        // 5. Get target channel group
        ChannelGroup channelGroup = SessionUtil.getChannelGroup(messageRequestPacket.getToUserId());

        // 6. Send to target
        if (channelGroup != null && !channelGroup.isEmpty()) {
            channelGroup.writeAndFlush(messageResponsePacket);
        } else {
            // Store offline message logic here
            System.err.println("User [" + messageRequestPacket.getToUserId() + "] is offline. Message stored.");
        }
    }
}
