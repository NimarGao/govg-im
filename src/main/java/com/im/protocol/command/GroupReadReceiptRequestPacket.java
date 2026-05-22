package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupReadReceiptRequestPacket extends Packet {
    private String groupId;
    private String msgId;

    @Override
    public Byte getCommand() {
        return Command.GROUP_READ_RECEIPT_REQUEST;
    }
}
