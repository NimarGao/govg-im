package com.im.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.im.protocol.command.MessageResponsePacket;
import com.im.util.SessionUtil;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

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
                
                channelGroup.writeAndFlush(packet);
            } else {
                // Not online on this instance; ignore
                // System.out.println("Redis Route Skip: User [" + toUserId + "] is not online on this server instance.");
            }
        } catch (Exception e) {
            System.err.println("Failed to process routed message from Redis PubSub:");
            e.printStackTrace();
        }
    }
}
