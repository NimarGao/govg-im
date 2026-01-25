package com.im.util;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class SessionUtil {
    private static final Map<String, ChannelGroup> userIdChannelGroupMap = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> groupIdUserIdsMap = new ConcurrentHashMap<>();
    
    private static final AttributeKey<Session> SESSION = AttributeKey.newInstance("session");

    public static void bindSession(Session session, Channel channel) {
        userIdChannelGroupMap.computeIfAbsent(session.getUserId(), k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE))
                .add(channel);
        channel.attr(SESSION).set(session);
    }

    public static void unbindSession(Channel channel) {
        if (hasLogin(channel)) {
            Session session = getSession(channel);
            ChannelGroup channelGroup = userIdChannelGroupMap.get(session.getUserId());
            if (channelGroup != null) {
                channelGroup.remove(channel);
                if (channelGroup.isEmpty()) {
                    userIdChannelGroupMap.remove(session.getUserId());
                }
            }
            channel.attr(SESSION).set(null);
        }
    }

    public static boolean hasLogin(Channel channel) {
        return channel.hasAttr(SESSION) && channel.attr(SESSION).get() != null;
    }

    public static Session getSession(Channel channel) {
        return channel.attr(SESSION).get();
    }

    public static ChannelGroup getChannelGroup(String userId) {
        return userIdChannelGroupMap.get(userId);
    }

    // Group methods
    public static void bindChannelGroup(String groupId, String userId) {
        groupIdUserIdsMap.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(userId);
    }

    public static Set<String> getGroupMembers(String groupId) {
        return groupIdUserIdsMap.getOrDefault(groupId, new CopyOnWriteArraySet<>());
    }
}
