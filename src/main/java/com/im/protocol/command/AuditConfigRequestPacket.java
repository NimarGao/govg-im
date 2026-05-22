package com.im.protocol.command;

import com.im.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditConfigRequestPacket extends Packet {
    private String policy; // "MASK" | "BLOCK"
    private List<String> sensitiveWords;

    @Override
    public Byte getCommand() {
        return Command.AUDIT_CONFIG_REQUEST;
    }
}
