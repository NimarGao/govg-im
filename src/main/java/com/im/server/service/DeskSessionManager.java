package com.im.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeskSessionManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SESSION_KEY_PREFIX = "im:desk:session:";
    private static final String AGENT_LOAD_KEY = "im:desk:agent:load";
    private static final String USER_RATING_LOG = "im:desk:ratings";
    
    // We pre-define two mock agents for customer desk service
    public static final List<Map<String, String>> STATIC_AGENTS = Arrays.asList(
        Map.of("id", "agent_001", "name", "金牌客服-安琪"),
        Map.of("id", "agent_002", "name", "资深客服-阿力")
    );

    /**
     * Get the active human agent ID mapped to the user.
     * Returns null if user is in robot FAQ session.
     */
    public String getAssignedAgent(String userId) {
        return redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + userId);
    }

    /**
     * Dispatch an online human customer agent using least-connections load balancing.
     * Returns null if no agents are online.
     */
    public Map<String, String> dispatchAgent(String userId) {
        List<Map<String, String>> onlineAgents = new ArrayList<>();
        
        // Check which static agents are online by checking their session key
        for (Map<String, String> agent : STATIC_AGENTS) {
            String agentId = agent.get("id");
            String sessionKey = "im:session:" + agentId;
            
            // If the agent is online on any Netty server, count them as available
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                onlineAgents.add(agent);
            }
        }

        if (onlineAgents.isEmpty()) {
            return null; // No human agents currently online
        }

        // Find the online agent with the minimum active session load
        Map<String, String> bestAgent = null;
        int minLoad = Integer.MAX_VALUE;

        for (Map<String, String> agent : onlineAgents) {
            String agentId = agent.get("id");
            String loadStr = (String) redisTemplate.opsForHash().get(AGENT_LOAD_KEY, agentId);
            int load = (loadStr != null) ? Integer.parseInt(loadStr) : 0;
            
            if (load < minLoad) {
                minLoad = load;
                bestAgent = agent;
            }
        }

        if (bestAgent != null) {
            String agentId = bestAgent.get("id");
            // 1. Establish the session mapping in Redis
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + userId, agentId);
            // 2. Increment active session count for this agent
            redisTemplate.opsForHash().increment(AGENT_LOAD_KEY, agentId, 1);
            
            // 3. Mark the user as tied to this agent for reverse routing
            redisTemplate.opsForValue().set("im:desk:agent_client:" + agentId + ":" + userId, "active");
        }

        return bestAgent;
    }

    /**
     * Terminate customer service session and decrement load.
     */
    public void terminateSession(String userId) {
        String agentId = getAssignedAgent(userId);
        if (agentId != null) {
            redisTemplate.delete(SESSION_KEY_PREFIX + userId);
            redisTemplate.delete("im:desk:agent_client:" + agentId + ":" + userId);
            
            // Decrement active load
            String loadStr = (String) redisTemplate.opsForHash().get(AGENT_LOAD_KEY, agentId);
            int load = (loadStr != null) ? Integer.parseInt(loadStr) : 0;
            if (load > 0) {
                redisTemplate.opsForHash().put(AGENT_LOAD_KEY, agentId, String.valueOf(load - 1));
            } else {
                redisTemplate.opsForHash().put(AGENT_LOAD_KEY, agentId, "0");
            }
        }
    }
}
