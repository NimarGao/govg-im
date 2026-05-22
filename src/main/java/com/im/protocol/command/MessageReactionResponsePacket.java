package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionResponsePacket extends Packet {
    private String msgId;
    private String fromUserId;
    private String fromUserName;
    private String emoji;
    private String action; // "add" | "remove"
    private String toUserId;
    private String toGroupId;

    @Override
    public Byte getCommand() {
        return Command.MESSAGE_REACTION_RESPONSE;
    }
}
