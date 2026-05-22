package com.im.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TIMPushService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void pushOffline(String senderId, String recipientId, String title, String body) {
        try {
            // 1. Insert push record into database
            String sql = "INSERT INTO im_push_log (user_id, sender_id, title, body, status, create_time) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, recipientId, senderId, title, body, "SENT", new Date());

            // 2. Fetch sender username to enrich the notification
            String profileKey = "im:user:profile:" + senderId;
            String senderName = redisTemplate.opsForValue().get(profileKey);
            if (senderName == null || senderName.isEmpty()) {
                senderName = senderId;
            }

            // 3. Store real-time push event in Redis list for quick visual logs in Dev Console
            String pushEventJson = String.format(
                "{\"userId\":\"%s\",\"senderId\":\"%s\",\"senderName\":\"%s\",\"title\":\"%s\",\"body\":\"%s\",\"status\":\"SENT\",\"time\":%d}",
                recipientId, senderId, senderName, title.replace("\"", "\\\""), body.replace("\"", "\\\""), System.currentTimeMillis()
            );
            redisTemplate.opsForList().leftPush("im:push:events", pushEventJson);
            // Cap the size of push logs in Redis
            redisTemplate.opsForList().trim("im:push:events", 0, 99);
            
            System.out.println("TIMPush Service: Logged offline push from " + senderId + " to " + recipientId);
        } catch (Exception e) {
            System.err.println("TIMPush Service: Failed to record offline push: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getPushLogs(String userId, int limit) {
        String sql = "SELECT id, user_id as userId, sender_id as senderId, title, body, status, create_time as createTime " +
                     "FROM im_push_log WHERE user_id = ? ORDER BY create_time DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, userId, limit);
    }
    
    public List<Map<String, Object>> getAllPushLogs(int limit) {
        String sql = "SELECT id, user_id as userId, sender_id as senderId, title, body, status, create_time as createTime " +
                     "FROM im_push_log ORDER BY create_time DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }
}
