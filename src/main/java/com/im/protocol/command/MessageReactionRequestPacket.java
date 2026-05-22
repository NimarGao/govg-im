package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionRequestPacket extends Packet {
    private String msgId;
    private String emoji;
    private String toUserId;
    private String toGroupId;
    private String action; // "add" | "remove"

    @Override
    public Byte getCommand() {
        return Command.MESSAGE_REACTION_REQUEST;
    }
}
