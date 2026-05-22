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
