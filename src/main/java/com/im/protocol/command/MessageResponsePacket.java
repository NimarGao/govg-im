package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;

@Data
public class MessageResponsePacket extends Packet {
    private String fromUserId;
    private String fromUserName;
    private String message;
    private String msgId;
    private Integer msgType = 1;

    @Override
    public Byte getCommand() {
        return Command.MESSAGE_RESPONSE;
    }
}
