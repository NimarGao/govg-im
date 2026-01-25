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

    @Override
    public Byte getCommand() {
        return Command.ACK_PACKET;
    }
}
