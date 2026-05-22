package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageRequestPacket extends Packet {
    private String toGroupId;
    private String message;
    private String msgId;
    private Integer msgType = 1;
    
    // Quoting fields
    private String quoteMsgId;
    private String quoteSender;
    private String quoteContent;

    public GroupMessageRequestPacket(String toGroupId, String message) {
        this.toGroupId = toGroupId;
        this.message = message;
    }

    @Override
    public Byte getCommand() {
        return Command.GROUP_MESSAGE_REQUEST;
    }
}
