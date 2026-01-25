package com.im.protocol;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public abstract class Packet {
    /**
     * Protocol version
     */
    @JSONField(deserialize = false, serialize = false)
    private Byte version = 1;

    /**
     * Get the command type
     */
    @JSONField(deserialize = false, serialize = true)
    public abstract Byte getCommand();
}
