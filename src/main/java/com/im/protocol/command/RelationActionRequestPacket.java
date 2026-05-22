package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationActionRequestPacket extends Packet {
    private String action; // "add_blacklist" | "remove_blacklist" | "delete_friend"
    private String userId;
    private String targetUserId;

    @Override
    public Byte getCommand() {
        return Command.RELATION_ACTION_REQUEST;
    }
}
