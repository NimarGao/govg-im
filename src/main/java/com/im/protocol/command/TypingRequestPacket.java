package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingRequestPacket extends Packet {
    private String toUserId; // Target user who is currently chatting with the typist

    @Override
    public Byte getCommand() {
        return Command.TYPING_REQUEST;
    }
}
