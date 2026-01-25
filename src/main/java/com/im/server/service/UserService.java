package com.im.server.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    // Mock DB
    private static final Map<String, List<String>> userFriendsMap = new ConcurrentHashMap<>();

    static {
        // Initialize some mock data
        userFriendsMap.put("user1", Arrays.asList("user2", "user3"));
        userFriendsMap.put("user2", Arrays.asList("user1"));
        userFriendsMap.put("user3", Arrays.asList("user1"));
    }

    public List<String> getFriends(String userId) {
        return userFriendsMap.getOrDefault(userId, Collections.emptyList());
    }
}
