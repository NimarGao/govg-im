package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.Data;

@Data
public class LoginRequestPacket extends Packet {
    private String userId;
    private String username;
    private String password;
    private String token;

    @Override
    public Byte getCommand() {
        return Command.LOGIN_REQUEST;
    }
}
