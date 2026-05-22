package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingResponsePacket extends Packet {
    private String fromUserId; // The user who is currently typing

    @Override
    public Byte getCommand() {
        return Command.TYPING_RESPONSE;
    }
}
