package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;

@Data
public class LoginResponsePacket extends Packet {
    private boolean success;
    private String reason;
    private String userId;
    private String username;

    @Override
    public Byte getCommand() {
        return Command.LOGIN_RESPONSE;
    }
}
