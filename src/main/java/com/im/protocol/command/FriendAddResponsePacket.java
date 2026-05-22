package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendAddResponsePacket extends Packet {
    private String userId;
    private String targetUserId;
    private String remark;
    private String status; // "pending" | "accepted" | "rejected"
    private String requestsListJson; // JSON representation of pending friend requests

    @Override
    public Byte getCommand() {
        return Command.FRIEND_ADD_RESPONSE;
    }
}
