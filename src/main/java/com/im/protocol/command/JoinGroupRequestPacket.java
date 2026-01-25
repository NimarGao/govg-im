package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;

@Data
public class JoinGroupRequestPacket extends Packet {
    private String groupId;

    @Override
    public Byte getCommand() {
        return Command.JOIN_GROUP_REQUEST;
    }
}
