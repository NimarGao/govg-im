package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeskRequestPacket extends Packet {
    private String userId;
    private String action; // "get_welcome" | "ask_robot" | "transfer_agent" | "submit_rate" | "close_session"
    private String robotMsg; // message sent to robot
    private Integer score; // rating score (1-5)
    private String feedback; // evaluation feedback
    private String msgId; // associated message ID for evaluations

    @Override
    public Byte getCommand() {
        return Command.CUSTOM_DESK_REQUEST;
    }
}
