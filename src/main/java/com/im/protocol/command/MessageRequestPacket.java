package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageRequestPacket extends Packet {
    private String toUserId;
    private String message;
    private String msgId; // Added for idempotency
    private Integer msgType = 1; // Default 1: Text
    
    // Quoting fields
    private String quoteMsgId;
    private String quoteSender;
    private String quoteContent;
    
    public MessageRequestPacket(String toUserId, String message) {
        this.toUserId = toUserId;
        this.message = message;
    }

    public MessageRequestPacket(String toUserId, String message, String msgId) {
        this.toUserId = toUserId;
        this.message = message;
        this.msgId = msgId;
    }

    @Override
    public Byte getCommand() {
        return Command.MESSAGE_REQUEST;
    }
}
