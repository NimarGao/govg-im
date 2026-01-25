package com.im.util.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Session Prefix
    private static final String SESSION_PREFIX = "im:session:";

    public void saveSession(String userId, String nettyServerIp, int port) {
        String key = SESSION_PREFIX + userId;
        String value = nettyServerIp + ":" + port;
        redisTemplate.opsForValue().set(key, value, 7, TimeUnit.DAYS);
    }

    public String getSession(String userId) {
        return redisTemplate.opsForValue().get(SESSION_PREFIX + userId);
    }

    public void removeSession(String userId) {
        redisTemplate.delete(SESSION_PREFIX + userId);
    }
    
    // For incremental ID
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }
}
