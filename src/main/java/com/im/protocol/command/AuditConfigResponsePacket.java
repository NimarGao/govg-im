package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditConfigResponsePacket extends Packet {
    private boolean success;
    private String policy;
    private List<String> sensitiveWords;

    @Override
    public Byte getCommand() {
        return Command.AUDIT_CONFIG_RESPONSE;
    }
}
