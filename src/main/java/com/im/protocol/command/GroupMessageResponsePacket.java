package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;

@Data
public class GroupMessageResponsePacket extends Packet {
    private String fromGroupId;
    private String fromUser; // UserId
    private String message;
    private String msgId; // Original MsgId
    private Integer msgType = 1;

    @Override
    public Byte getCommand() {
        return Command.GROUP_MESSAGE_RESPONSE;
    }
}
