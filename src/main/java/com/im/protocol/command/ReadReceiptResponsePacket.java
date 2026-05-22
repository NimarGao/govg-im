package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptResponsePacket extends Packet {
    private String fromUserId;    // The user who marked the messages as read
    private String lastReadMsgId;  // Up to which message ID they have read

    @Override
    public Byte getCommand() {
        return Command.READ_RECEIPT_RESPONSE;
    }
}
