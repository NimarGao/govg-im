package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiVoiceCallResponsePacket extends Packet {
    private String groupId;
    private String action; // "start" | "join" | "leave" | "toggle_mute"
    private String userId;
    private Boolean mute;
    private String members; // JSON string representing list of { userId, mute }

    @Override
    public Byte getCommand() {
        return Command.MULTI_VOICE_CALL_RESPONSE;
    }
}
