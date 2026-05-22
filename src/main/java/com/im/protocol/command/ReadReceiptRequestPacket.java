package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptRequestPacket extends Packet {
    private String toUserId;     // User whose messages are marked as read
    private String lastReadMsgId; // The last message ID read by me

    @Override
    public Byte getCommand() {
        return Command.READ_RECEIPT_REQUEST;
    }
}
