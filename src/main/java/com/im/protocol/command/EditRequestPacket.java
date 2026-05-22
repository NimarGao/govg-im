package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditRequestPacket extends Packet {
    private String msgId;
    private String toUserId;
    private String toGroupId;
    private String newContent;

    @Override
    public Byte getCommand() {
        return Command.EDIT_REQUEST;
    }
}
