package com.im.server.controller.external;

import com.alibaba.fastjson.JSONObject;
import com.im.entity.Message;
import com.im.mapper.MessageMapper;
import com.im.config.RedisPubSubConfig;
import com.im.server.service.TIMPushService;
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

    @Autowired
    private TIMPushService timPushService;

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

        // Fetch custom user status from Redis Hash
        try {
            String customStatusVal = (String) redisTemplate.opsForHash().get("im:user:custom_status", userId);
            if (customStatusVal != null) {
                com.alibaba.fastjson.JSONObject customStatus = com.alibaba.fastjson.JSON.parseObject(customStatusVal);
                response.put("statusType", customStatus.getString("statusType"));
                response.put("statusText", customStatus.getString("statusText"));
            } else {
                response.put("statusType", "online");
                response.put("statusText", "");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch custom status for " + userId + ": " + e.getMessage());
            response.put("statusType", "online");
            response.put("statusText", "");
        }

        return response;
    }

    /**
     * Get real members list for a group.
     */
    @GetMapping("/group/members")
    public Map<String, Object> getGroupMembers(@RequestParam("groupId") String groupId) {
        Map<String, Object> response = new HashMap<>();

        if (groupId == null || groupId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "groupId is required");
            return response;
        }

        String groupKey = "im:group:members:" + groupId;
        java.util.Set<String> memberIds = redisTemplate.opsForSet().members(groupKey);

        java.util.List<Map<String, Object>> membersList = new java.util.ArrayList<>();
        if (memberIds != null) {
            for (String userId : memberIds) {
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", userId);

                // Fetch real nickname
                String profileKey = "im:user:profile:" + userId;
                String username = redisTemplate.opsForValue().get(profileKey);
                memberInfo.put("username", username != null ? username : userId);

                // Online state
                String sessionKey = SESSION_PREFIX + userId;
                boolean online = redisTemplate.hasKey(sessionKey);
                memberInfo.put("online", online);

                membersList.add(memberInfo);
            }
        }

        response.put("success", true);
        response.put("members", membersList);
        return response;
    }

    /**
     * Get historical messages.
     */
    @GetMapping("/message/history")
    public Map<String, Object> getMessageHistory(
            @RequestParam("userId") String userId,
            @RequestParam("peerId") String peerId,
            @RequestParam("type") Integer type,
            @RequestParam(value = "beforeTime", required = false) Long beforeTime,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (userId == null || userId.trim().isEmpty() || peerId == null || peerId.trim().isEmpty() || type == null) {
            response.put("success", false);
            response.put("message", "Missing required parameters");
            return response;
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        
        if (type == 1) {
            // Single chat: (from = A AND to = B) OR (from = B AND to = A)
            queryWrapper.and(wrapper -> wrapper
                .eq("from_user_id", userId).eq("to_user_id", peerId).eq("type", 1)
                .or()
                .eq("from_user_id", peerId).eq("to_user_id", userId).eq("type", 1)
            );
        } else {
            // Group chat: to_user_id = peerId
            queryWrapper.eq("to_user_id", peerId).eq("type", 2);
        }

        if (beforeTime != null && beforeTime > 0) {
            queryWrapper.lt("create_time", new Date(beforeTime));
        }

        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT " + limit);

        java.util.List<Message> list = messageMapper.selectList(queryWrapper);
        
        // Reverse list to be in chronological order
        java.util.Collections.reverse(list);

        // Fetch reactions from Redis for each message
        if (list != null) {
            for (Message msg : list) {
                try {
                    String redisKey = "im:message:reactions:" + msg.getMsgId();
                    java.util.Map<Object, Object> reactionMap = redisTemplate.opsForHash().entries(redisKey);
                    if (reactionMap != null && !reactionMap.isEmpty()) {
                        java.util.Map<String, java.util.List<java.util.Map<String, String>>> reactions = new java.util.HashMap<>();
                        for (java.util.Map.Entry<Object, Object> entry : reactionMap.entrySet()) {
                            String rUserId = (String) entry.getKey();
                            String emoji = (String) entry.getValue();
                            
                            // Fetch username from Redis
                            String profileKey = "im:user:profile:" + rUserId;
                            String rUsername = redisTemplate.opsForValue().get(profileKey);
                            if (rUsername == null || rUsername.isEmpty()) {
                                rUsername = rUserId;
                            }
                            
                            java.util.Map<String, String> userObj = new java.util.HashMap<>();
                            userObj.put("userId", rUserId);
                            userObj.put("userName", rUsername);
                            
                            reactions.computeIfAbsent(emoji, k -> new java.util.ArrayList<>()).add(userObj);
                        }
                        msg.setReactions(reactions);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch reactions for message " + msg.getMsgId() + ": " + e.getMessage());
                }
            }
        }

        response.put("success", true);
        response.put("messages", list);
        return response;
    }

    /**
     * Get offline push history logs.
     */
    @GetMapping("/push/history")
    public Map<String, Object> getPushHistory(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "limit", defaultValue = "50") Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            java.util.List<Map<String, Object>> logs;
            if (userId != null && !userId.trim().isEmpty()) {
                logs = timPushService.getPushLogs(userId, limit);
            } else {
                logs = timPushService.getAllPushLogs(limit);
            }
            response.put("success", true);
            response.put("logs", logs);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get friends list.
     */
    @GetMapping("/friends")
    public Map<String, Object> getFriends(@RequestParam("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        if (userId == null || userId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "userId is required");
            return response;
        }

        try {
            java.util.Set<String> friendIds = redisTemplate.opsForSet().members("im:user:friends:" + userId);
            java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
            if (friendIds != null) {
                for (String friendId : friendIds) {
                    Map<String, Object> friendMap = new HashMap<>();
                    friendMap.put("id", friendId);

                    // Name
                    String profileKey = "im:user:profile:" + friendId;
                    String name = redisTemplate.opsForValue().get(profileKey);
                    friendMap.put("name", name != null && !name.isEmpty() ? name : friendId);

                    // Online
                    String sessionKey = SESSION_PREFIX + friendId;
                    boolean online = Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
                    friendMap.put("online", online);

                    // Custom Status
                    String statusType = "online";
                    String statusText = "";
                    String customStatusVal = (String) redisTemplate.opsForHash().get("im:user:custom_status", friendId);
                    if (customStatusVal != null) {
                        try {
                            com.alibaba.fastjson.JSONObject customStatus = com.alibaba.fastjson.JSON.parseObject(customStatusVal);
                            statusType = customStatus.getString("statusType");
                            statusText = customStatus.getString("statusText");
                        } catch (Exception ignored) {}
                    }
                    friendMap.put("statusType", statusType);
                    friendMap.put("statusText", statusText);
                    friendMap.put("node", online ? redisTemplate.opsForValue().get(sessionKey) : "N/A");

                    list.add(friendMap);
                }
            }
            response.put("success", true);
            response.put("friends", list);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get blacklist.
     */
    @GetMapping("/blacklist")
    public Map<String, Object> getBlacklist(@RequestParam("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        if (userId == null || userId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "userId is required");
            return response;
        }

        try {
            java.util.Set<String> blackIds = redisTemplate.opsForSet().members("im:user:blacklist:" + userId);
            java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
            if (blackIds != null) {
                for (String blackId : blackIds) {
                    Map<String, Object> blackMap = new HashMap<>();
                    blackMap.put("id", blackId);

                    // Name
                    String profileKey = "im:user:profile:" + blackId;
                    String name = redisTemplate.opsForValue().get(profileKey);
                    blackMap.put("name", name != null && !name.isEmpty() ? name : blackId);

                    // Online
                    String sessionKey = SESSION_PREFIX + blackId;
                    boolean online = Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
                    blackMap.put("online", online);

                    list.add(blackMap);
                }
            }
            response.put("success", true);
            response.put("blacklist", list);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Get pending friend requests.
     */
    @GetMapping("/friend_requests")
    public Map<String, Object> getFriendRequests(@RequestParam("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        if (userId == null || userId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "userId is required");
            return response;
        }

        try {
            String key = "im:user:friend_requests:" + userId;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
            for (Object val : entries.values()) {
                try {
                    com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject((String) val);
                    Map<String, Object> reqMap = new HashMap<>();
                    reqMap.put("userId", obj.getString("userId"));
                    reqMap.put("targetUserId", obj.getString("targetUserId"));
                    reqMap.put("remark", obj.getString("remark"));
                    reqMap.put("status", obj.getString("status"));
                    reqMap.put("createTime", obj.getLong("createTime"));

                    // Get requester's nickname
                    String requesterId = obj.getString("userId");
                    String profileKey = "im:user:profile:" + requesterId;
                    String name = redisTemplate.opsForValue().get(profileKey);
                    reqMap.put("username", name != null && !name.isEmpty() ? name : requesterId);

                    list.add(reqMap);
                } catch (Exception ignored) {}
            }
            response.put("success", true);
            response.put("requests", list);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
