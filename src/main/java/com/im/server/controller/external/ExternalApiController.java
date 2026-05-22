package com.im.server.controller.external;

import com.alibaba.fastjson.JSONObject;
import com.im.entity.Message;
import com.im.mapper.MessageMapper;
import com.im.config.RedisPubSubConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/external")
public class ExternalApiController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MessageMapper messageMapper;

    // Token redis prefix
    private static final String TOKEN_PREFIX = "im:token:";
    // Session redis prefix
    private static final String SESSION_PREFIX = "im:session:";

    /**
     * Generate secure IM connection token for a user.
     * Called by external backend.
     */
    @PostMapping("/auth/token")
    public Map<String, Object> generateToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String userId = request.get("userId");
        String username = request.get("username");

        if (userId == null || userId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "userId is required");
            return response;
        }

        if (username == null || username.trim().isEmpty()) {
            username = userId;
        }

        // Generate secure random token
        String token = "im_tk_" + UUID.randomUUID().toString().replace("-", "");
        String redisKey = TOKEN_PREFIX + token;
        String redisVal = userId + ":" + username;

        // Store token in Redis, expires in 24 hours
        redisTemplate.opsForValue().set(redisKey, redisVal, 24, TimeUnit.HOURS);

        response.put("success", true);
        response.put("token", token);
        response.put("expiresIn", 86400); // seconds
        return response;
    }

    /**
     * Send direct push message to a client (routes across Netty instances using Redis Pub/Sub).
     * Called by external backend.
     */
    @PostMapping("/message/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        String toUserId = (String) request.get("toUserId");
        String msgContent = (String) request.get("message");

        if (toUserId == null || toUserId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "toUserId is required");
            return response;
        }

        if (msgContent == null || msgContent.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "message content is required");
            return response;
        }

        String fromUserId = (String) request.getOrDefault("fromUserId", "system");
        String fromUserName = (String) request.getOrDefault("fromUserName", "系统通知");
        Integer msgType = (Integer) request.getOrDefault("msgType", 1); // 1: Text

        String msgId = "msg-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        // 1. Persist message in database
        try {
            Message message = new Message();
            message.setMsgId(msgId);
            message.setFromUserId(fromUserId);
            message.setToUserId(toUserId);
            message.setContent(msgContent);
            message.setType(1); // 1: Single chat (system-to-user)
            message.setMsgType(msgType);
            message.setStatus(0); // Sent
            message.setCreateTime(new Date());
            messageMapper.insert(message);
        } catch (Exception e) {
            System.err.println("Failed to persist external message in DB: " + e.getMessage());
        }

        // 2. Publish to Redis Pub/Sub for clustered Netty routing
        JSONObject pubSubMessage = new JSONObject();
        pubSubMessage.put("toUserId", toUserId);
        pubSubMessage.put("fromUserId", fromUserId);
        pubSubMessage.put("fromUserName", fromUserName);
        pubSubMessage.put("message", msgContent);
        pubSubMessage.put("msgId", msgId);
        pubSubMessage.put("msgType", msgType);

        redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, pubSubMessage.toJSONString());

        response.put("success", true);
        response.put("msgId", msgId);
        return response;
    }

    /**
     * Check if user is currently online.
     * Called by external backend.
     */
    @GetMapping("/user/status")
    public Map<String, Object> getUserStatus(@RequestParam("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        
        if (userId == null || userId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "userId is required");
            return response;
        }

        String redisKey = SESSION_PREFIX + userId;
        String sessionVal = redisTemplate.opsForValue().get(redisKey);

        if (sessionVal != null) {
            response.put("success", true);
            response.put("online", true);
            response.put("server", sessionVal); // netty server IP:Port
        } else {
            response.put("success", true);
            response.put("online", false);
        }

        return response;
    }
}
