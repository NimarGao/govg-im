package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeskResponsePacket extends Packet {
    private String action; // matches the request action
    private String status; // "success" | "failed"
    private String agentId; // assigned customer service agent ID
    private String agentName; // agent nickname (e.g. "安琪")
    private Integer msgType; // 6 for smart desk interactive cards
    private String content; // JSON content string containing robot/agent reply & options list

    @Override
    public Byte getCommand() {
        return Command.CUSTOM_DESK_RESPONSE;
    }
}
