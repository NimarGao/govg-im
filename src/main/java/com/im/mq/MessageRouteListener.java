package com.im.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.im.protocol.command.*;
import com.im.util.SessionUtil;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MessageRouteListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Raw JSON string published by StringRedisTemplate
            String jsonStr = new String(message.getBody());
            
            // To handle potential String serialization wrapping in Redis if standard serialization was used
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = JSON.parse(jsonStr).toString();
            }

            JSONObject jsonObject = JSON.parseObject(jsonStr);

            // Check if there is a specific command field (used for control packets like Read Receipt & Typing)
            if (jsonObject.containsKey("command")) {
                Byte command = jsonObject.getByte("command");
                if (Command.READ_RECEIPT_RESPONSE.equals(command)) {
                    String toUserId = jsonObject.getString("toUserId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    String lastReadMsgId = jsonObject.getString("lastReadMsgId");

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding ReadReceiptResponse to User [" + toUserId + "] from User [" + fromUserId + "]");
                        ReadReceiptResponsePacket packet = new ReadReceiptResponsePacket(fromUserId, lastReadMsgId);
                        channelGroup.writeAndFlush(packet);
                    }
                    return;
                } else if (Command.TYPING_RESPONSE.equals(command)) {
                    String toUserId = jsonObject.getString("toUserId");
                    String fromUserId = jsonObject.getString("fromUserId");

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding TypingResponse to User [" + toUserId + "] from User [" + fromUserId + "]");
                        TypingResponsePacket packet = new TypingResponsePacket(fromUserId);
                        channelGroup.writeAndFlush(packet);
                    }
                    return;
                } else if (Command.RECALL_RESPONSE.equals(command)) {
                    String msgId = jsonObject.getString("msgId");
                    String toUserId = jsonObject.getString("toUserId");
                    String toGroupId = jsonObject.getString("toGroupId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    boolean success = jsonObject.getBooleanValue("success");

                    RecallResponsePacket packet = new RecallResponsePacket(msgId, toUserId, toGroupId, fromUserId, success, null);
                    
                    if (toGroupId != null && !toGroupId.isEmpty()) {
                        Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                if (memberId.equals(fromUserId)) {
                                    continue;
                                }
                                ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                                if (channelGroup != null && !channelGroup.isEmpty()) {
                                    channelGroup.writeAndFlush(packet);
                                }
                            }
                        }
                    } else if (toUserId != null && !toUserId.isEmpty()) {
                        ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                        if (channelGroup != null && !channelGroup.isEmpty()) {
                            channelGroup.writeAndFlush(packet);
                        }
                    }
                    return;
                } else if (Command.EDIT_RESPONSE.equals(command)) {
                    String msgId = jsonObject.getString("msgId");
                    String toUserId = jsonObject.getString("toUserId");
                    String toGroupId = jsonObject.getString("toGroupId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    String newContent = jsonObject.getString("newContent");
                    boolean success = jsonObject.getBooleanValue("success");

                    EditResponsePacket packet = new EditResponsePacket(msgId, toUserId, toGroupId, fromUserId, newContent, success, null);

                    if (toGroupId != null && !toGroupId.isEmpty()) {
                        Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                if (memberId.equals(fromUserId)) {
                                    continue;
                                }
                                ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                                if (channelGroup != null && !channelGroup.isEmpty()) {
                                    channelGroup.writeAndFlush(packet);
                                }
                            }
                        }
                    } else if (toUserId != null && !toUserId.isEmpty()) {
                        ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                        if (channelGroup != null && !channelGroup.isEmpty()) {
                            channelGroup.writeAndFlush(packet);
                        }
                    }
                    return;
                } else if (Command.GROUP_READ_RECEIPT_RESPONSE.equals(command)) {
                    String groupId = jsonObject.getString("groupId");
                    String msgId = jsonObject.getString("msgId");
                    int readCount = jsonObject.getIntValue("readCount");
                    
                    com.alibaba.fastjson.JSONArray array = jsonObject.getJSONArray("readUserIds");
                    java.util.Set<String> readUserIds = new java.util.HashSet<>();
                    if (array != null) {
                        for (int i = 0; i < array.size(); i++) {
                            readUserIds.add(array.getString(i));
                        }
                    }

                    GroupReadReceiptResponsePacket packet = new GroupReadReceiptResponsePacket(groupId, msgId, readCount, readUserIds);

                    Set<String> memberIds = SessionUtil.getGroupMembers(groupId);
                    if (memberIds != null) {
                        for (String memberId : memberIds) {
                            ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                            if (channelGroup != null && !channelGroup.isEmpty()) {
                                channelGroup.writeAndFlush(packet);
                            }
                        }
                    }
                    return;
                } else if (Command.MESSAGE_REACTION_RESPONSE.equals(command)) {
                    String msgId = jsonObject.getString("msgId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    String fromUserName = jsonObject.getString("fromUserName");
                    String emoji = jsonObject.getString("emoji");
                    String action = jsonObject.getString("action");
                    String toUserId = jsonObject.getString("toUserId");
                    String toGroupId = jsonObject.getString("toGroupId");

                    MessageReactionResponsePacket packet = new MessageReactionResponsePacket(
                        msgId, fromUserId, fromUserName, emoji, action, toUserId, toGroupId
                    );

                    if (toGroupId != null && !toGroupId.isEmpty()) {
                        Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                                if (channelGroup != null && !channelGroup.isEmpty()) {
                                    channelGroup.writeAndFlush(packet);
                                }
                            }
                        }
                    } else {
                        // Send to both recipient and sender if they are online on this Netty node
                        if (toUserId != null && !toUserId.isEmpty()) {
                            ChannelGroup recipientGroup = SessionUtil.getChannelGroup(toUserId);
                            if (recipientGroup != null && !recipientGroup.isEmpty()) {
                                recipientGroup.writeAndFlush(packet);
                            }
                        }
                        if (fromUserId != null && !fromUserId.isEmpty()) {
                            ChannelGroup senderGroup = SessionUtil.getChannelGroup(fromUserId);
                            if (senderGroup != null && !senderGroup.isEmpty()) {
                                senderGroup.writeAndFlush(packet);
                            }
                        }
                    }
                    return;
                } else if (Command.VOICE_CALL_REQUEST.equals(command)) {
                    String callId = jsonObject.getString("callId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    String toUserId = jsonObject.getString("toUserId");
                    String callType = jsonObject.getString("callType");
                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding VoiceCallRequest to User [" + toUserId + "] from User [" + fromUserId + "]");
                        channelGroup.writeAndFlush(new VoiceCallRequestPacket(callId, fromUserId, toUserId, callType));
                    }
                    return;
                } else if (Command.VOICE_CALL_RESPONSE.equals(command)) {
                    String callId = jsonObject.getString("callId");
                    String fromUserId = jsonObject.getString("fromUserId");
                    String toUserId = jsonObject.getString("toUserId");
                    String action = jsonObject.getString("action");
                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding VoiceCallResponse to User [" + toUserId + "] from User [" + fromUserId + "] with action [" + action + "]");
                        channelGroup.writeAndFlush(new VoiceCallResponsePacket(callId, fromUserId, toUserId, action));
                    }
                    return;
                } else if (Command.USER_STATUS_RESPONSE.equals(command)) {
                    String toUserId = jsonObject.getString("toUserId");
                    String userId = jsonObject.getString("userId");
                    String statusType = jsonObject.getString("statusType");
                    String statusText = jsonObject.getString("statusText");
                    boolean online = jsonObject.getBooleanValue("online");

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding UserStatusResponse to User [" + toUserId + "] for User [" + userId + "]");
                        UserStatusResponsePacket packet = new UserStatusResponsePacket(userId, statusType, statusText, online);
                        channelGroup.writeAndFlush(packet);
                    }
                    return;
                } else if (Command.AUDIT_CONFIG_RESPONSE.equals(command) && jsonObject.getBooleanValue("syncAudit")) {
                    String policy = jsonObject.getString("policy");
                    com.alibaba.fastjson.JSONArray array = jsonObject.getJSONArray("sensitiveWords");
                    java.util.List<String> words = new java.util.ArrayList<>();
                    if (array != null) {
                        for (int i = 0; i < array.size(); i++) {
                            words.add(array.getString(i));
                        }
                    }
                    System.out.println("Redis Route Match: Syncing Content Auditing config locally (Policy: " + policy + ", Word Count: " + words.size() + ")");
                    com.im.util.SensitiveWordFilter.INSTANCE.setPolicy(policy);
                    com.im.util.SensitiveWordFilter.INSTANCE.setSensitiveWords(words);
                    return;
                } else if (Command.CUSTOM_DESK_RESPONSE.equals(command)) {
                    String toUserId = jsonObject.getString("toUserId");
                    String action = jsonObject.getString("action");
                    String status = jsonObject.getString("status");
                    String agentId = jsonObject.getString("agentId");
                    String agentName = jsonObject.getString("agentName");
                    Integer msgType = jsonObject.getInteger("msgType");
                    String content = jsonObject.getString("content");

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        System.out.println("Redis Route Match: Forwarding CUSTOM_DESK_RESPONSE to User [" + toUserId + "] with action [" + action + "]");
                        DeskResponsePacket response = new DeskResponsePacket(action, status, agentId, agentName, msgType, content);
                        channelGroup.writeAndFlush(response);
                    }
                    return;
                } else if (Command.MULTI_VOICE_CALL_RESPONSE.equals(command)) {
                    String groupId = jsonObject.getString("groupId");
                    String action = jsonObject.getString("action");
                    String userId = jsonObject.getString("userId");
                    Boolean mute = jsonObject.getBoolean("mute");
                    String members = jsonObject.getString("members");

                    System.out.println("Redis Route Match: Forwarding MultiVoiceCallResponse for Group [" + groupId + "]");
                    MultiVoiceCallResponsePacket responsePacket = new MultiVoiceCallResponsePacket(
                        groupId, action, userId, mute, members
                    );

                    Set<String> memberIds = SessionUtil.getGroupMembers(groupId);
                    if (memberIds != null) {
                        for (String memberId : memberIds) {
                            ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                            if (channelGroup != null && !channelGroup.isEmpty()) {
                                channelGroup.writeAndFlush(responsePacket);
                            }
                        }
                    }
                    return;
                } else if (Command.FRIEND_ADD_RESPONSE.equals(command)) {
                    String userId = jsonObject.getString("userId");
                    String targetUserId = jsonObject.getString("targetUserId");
                    String remark = jsonObject.getString("remark");
                    String status = jsonObject.getString("status");
                    String requestsListJson = jsonObject.getString("requestsListJson");

                    System.out.println("Redis Route Match: Forwarding FriendAddResponse to User [" + userId + "]");
                    FriendAddResponsePacket packet = new FriendAddResponsePacket(
                        userId, targetUserId, remark, status, requestsListJson
                    );

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(userId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        channelGroup.writeAndFlush(packet);
                    }
                    return;
                } else if (Command.RELATION_ACTION_RESPONSE.equals(command)) {
                    String action = jsonObject.getString("action");
                    String userId = jsonObject.getString("userId");
                    String targetUserId = jsonObject.getString("targetUserId");
                    Boolean success = jsonObject.getBoolean("success");
                    String blacklistJson = jsonObject.getString("blacklistJson");

                    System.out.println("Redis Route Match: Forwarding RelationActionResponse to User [" + userId + "] with action [" + action + "]");
                    RelationActionResponsePacket packet = new RelationActionResponsePacket(
                        action, userId, targetUserId, success, blacklistJson
                    );

                    ChannelGroup channelGroup = SessionUtil.getChannelGroup(userId);
                    if (channelGroup != null && !channelGroup.isEmpty()) {
                        channelGroup.writeAndFlush(packet);
                    }
                    return;
                }
            }

            // A. Handle Distributed Group Message Broadcast Route
            if (jsonObject.containsKey("toGroupId")) {
                String toGroupId = jsonObject.getString("toGroupId");
                String fromUser = jsonObject.getString("fromUser");
                String msgContent = jsonObject.getString("message");
                String msgId = jsonObject.getString("msgId");
                Integer msgType = jsonObject.getInteger("msgType");

                Set<String> memberIds = SessionUtil.getGroupMembers(toGroupId);
                if (memberIds != null && !memberIds.isEmpty()) {
                    GroupMessageResponsePacket responsePacket = new GroupMessageResponsePacket();
                    responsePacket.setFromGroupId(toGroupId);
                    responsePacket.setMessage(msgContent);
                    responsePacket.setFromUser(fromUser);
                    responsePacket.setMsgId(msgId);
                    responsePacket.setMsgType(msgType != null ? msgType : 1);
                    responsePacket.setQuoteMsgId(jsonObject.getString("quoteMsgId"));
                    responsePacket.setQuoteSender(jsonObject.getString("quoteSender"));
                    responsePacket.setQuoteContent(jsonObject.getString("quoteContent"));

                    for (String memberId : memberIds) {
                        // Skip sender to avoid echoed message (sender already renders locally)
                        if (memberId.equals(fromUser)) {
                            continue;
                        }
                        ChannelGroup channelGroup = SessionUtil.getChannelGroup(memberId);
                        if (channelGroup != null && !channelGroup.isEmpty()) {
                            System.out.println("Redis Group Route Match: User [" + memberId + "] in Group [" + toGroupId + "] is online on this instance. Pushing message.");
                            channelGroup.writeAndFlush(responsePacket);
                        }
                    }
                }
            }
            // B. Handle Distributed Single Message Route
            else {
                String toUserId = jsonObject.getString("toUserId");
                String fromUserId = jsonObject.getString("fromUserId");
                String fromUserName = jsonObject.getString("fromUserName");
                String msgContent = jsonObject.getString("message");
                String msgId = jsonObject.getString("msgId");
                Integer msgType = jsonObject.getInteger("msgType");

                // Look up target channel group in local session registry
                ChannelGroup channelGroup = SessionUtil.getChannelGroup(toUserId);

                if (channelGroup != null && !channelGroup.isEmpty()) {
                    System.out.println("Redis Route Match: User [" + toUserId + "] is online on this instance. Pushing message.");
                    
                    // Construct packet and push to client
                    MessageResponsePacket packet = new MessageResponsePacket();
                    packet.setFromUserId(fromUserId);
                    packet.setFromUserName(fromUserName);
                    packet.setMessage(msgContent);
                    packet.setMsgId(msgId);
                    packet.setMsgType(msgType != null ? msgType : 1);
                    packet.setQuoteMsgId(jsonObject.getString("quoteMsgId"));
                    packet.setQuoteSender(jsonObject.getString("quoteSender"));
                    packet.setQuoteContent(jsonObject.getString("quoteContent"));
                    
                    channelGroup.writeAndFlush(packet);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process routed message from Redis PubSub:");
            e.printStackTrace();
        }
    }
}
