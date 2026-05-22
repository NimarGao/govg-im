package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequestPacket extends Packet {
    private String userId;
    private String statusType; // "online" | "busy" | "away"
    private String statusText; // custom signature text, e.g., "☕ 摸鱼中"

    @Override
    public Byte getCommand() {
        return Command.USER_STATUS_REQUEST;
    }
}
