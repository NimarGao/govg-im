package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AckPacket extends Packet {
    private String msgId; // The message ID being acknowledged
    private String fromUserId; // Who is acking
    private Integer status = 0; // Status code: 0 = OK, 5001 = Blocked
    private String message;     // Optional status message

    public AckPacket(String msgId, String fromUserId) {
        this.msgId = msgId;
        this.fromUserId = fromUserId;
    }

    @Override
    public Byte getCommand() {
        return Command.ACK_PACKET;
    }
}
