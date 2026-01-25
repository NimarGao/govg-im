package com.im.server.handler;

import com.im.protocol.command.CreateGroupRequestPacket;
import com.im.protocol.command.CreateGroupResponsePacket;
import com.im.util.SessionUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ChannelHandler.Sharable
public class CreateGroupRequestHandler extends SimpleChannelInboundHandler<CreateGroupRequestPacket> {

    public static final CreateGroupRequestHandler INSTANCE = new CreateGroupRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CreateGroupRequestPacket createGroupRequestPacket) {
        List<String> userIdList = createGroupRequestPacket.getUserIdList();
        List<String> userNameList = new ArrayList<>();

        // Generate Group ID
        String groupId = UUID.randomUUID().toString().split("-")[0];

        // 1. Add creator (self) to the group
        String creatorId = SessionUtil.getSession(ctx.channel()).getUserId();
        String creatorName = SessionUtil.getSession(ctx.channel()).getUserName();
        SessionUtil.bindChannelGroup(groupId, creatorId);
        userNameList.add(creatorName);

        // 2. Add other members
        if (userIdList != null) {
            for (String userId : userIdList) {
                SessionUtil.bindChannelGroup(groupId, userId);
                userNameList.add(userId); // In real scenario, query DB for name
            }
        }

        // 3. Response
        CreateGroupResponsePacket createGroupResponsePacket = new CreateGroupResponsePacket();
        createGroupResponsePacket.setSuccess(true);
        createGroupResponsePacket.setGroupId(groupId);
        createGroupResponsePacket.setUserNameList(userNameList);

        ctx.channel().writeAndFlush(createGroupResponsePacket);
        
        System.out.println("Group created: " + groupId + ", members: " + userNameList);
    }
}
