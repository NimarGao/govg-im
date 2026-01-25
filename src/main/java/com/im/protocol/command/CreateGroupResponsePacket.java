package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupResponsePacket extends Packet {
    private boolean success;
    private String groupId;
    private List<String> userNameList;

    @Override
    public Byte getCommand() {
        return Command.CREATE_GROUP_RESPONSE;
    }
}
