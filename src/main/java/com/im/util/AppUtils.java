package com.im.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.springframework.stereotype.Component;

@Component
public class AppUtils {

    private static final Snowflake snowflake = IdUtil.getSnowflake(1, 1);
    private static final byte[] HMAC_KEY = "im-system-key".getBytes();

    public static String nextId() {
        return snowflake.nextIdStr();
    }

    public static String hmacSha256(String data) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, HMAC_KEY);
        return hMac.digestHex(data);
    }
}
