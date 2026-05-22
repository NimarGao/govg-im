package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCallRequestPacket extends Packet {
    private String callId;
    private String fromUserId;
    private String toUserId;
    private String callType; // "voice" | "video"

    @Override
    public Byte getCommand() {
        return Command.VOICE_CALL_REQUEST;
    }
}
