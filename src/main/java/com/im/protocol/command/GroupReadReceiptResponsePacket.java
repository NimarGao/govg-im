package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupReadReceiptResponsePacket extends Packet {
    private String groupId;
    private String msgId;
    private int readCount;
    private Set<String> readUserIds;

    @Override
    public Byte getCommand() {
        return Command.GROUP_READ_RECEIPT_RESPONSE;
    }
}
