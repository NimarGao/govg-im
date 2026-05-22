package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecallRequestPacket extends Packet {
    private String msgId;
    private String toUserId;
    private String toGroupId;

    @Override
    public Byte getCommand() {
        return Command.RECALL_REQUEST;
    }
}
