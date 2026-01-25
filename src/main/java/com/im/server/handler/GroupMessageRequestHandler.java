package com.im.server.handler;

import com.im.protocol.command.GroupMessageRequestPacket;
import com.im.protocol.command.GroupMessageResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

import java.util.Set;

@ChannelHandler.Sharable
public class GroupMessageRequestHandler extends SimpleChannelInboundHandler<GroupMessageRequestPacket> {

    public static final GroupMessageRequestHandler INSTANCE = new GroupMessageRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMessageRequestPacket requestPacket) {
        // 1. Get info
        String groupId = requestPacket.getToGroupId();
        Session session = SessionUtil.getSession(ctx.channel());

        // 2. Create Response
        GroupMessageResponsePacket responsePacket = new GroupMessageResponsePacket();
        responsePacket.setFromGroupId(groupId);
        responsePacket.setMessage(requestPacket.getMessage());
        responsePacket.setFromUser(session.getUserId());
        responsePacket.setMsgId(requestPacket.getMsgId());
        responsePacket.setMsgType(requestPacket.getMsgType());

        // 3. Get all members
        Set<String> memberIds = SessionUtil.getGroupMembers(groupId);

        // 4. Send to everyone except sender (optional: can also send to sender's other devices)
        for (String memberId : memberIds) {
            if (memberId.equals(session.getUserId())) {
                continue; // Skip self (or handle multi-device sync logic here)
            }
            
            ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                channelGroup.writeAndFlush(responsePacket);
            }
        }
    }
}
