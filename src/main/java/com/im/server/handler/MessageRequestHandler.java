package com.im.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.AckPacket;
import com.im.protocol.command.Command;
import com.im.protocol.command.MessageRequestPacket;
import com.im.protocol.command.MessageResponsePacket;
import com.im.server.service.DeskSessionManager;
import com.im.server.service.TIMPushService;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import com.im.util.redis.RedisService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

@ChannelHandler.Sharable
public class MessageRequestHandler extends SimpleChannelInboundHandler<MessageRequestPacket> {

    public static final MessageRequestHandler INSTANCE = new MessageRequestHandler();

    private MessageRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageRequestPacket messageRequestPacket) {
        // 1. Get current session
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String toUserId = messageRequestPacket.getToUserId();
        String originalContent = messageRequestPacket.getMessage();

        DeskSessionManager deskSessionManager = SpringContextHolder.getBean(DeskSessionManager.class);
        String assignedAgentId = null;
        if (deskSessionManager != null) {
            assignedAgentId = deskSessionManager.getAssignedAgent(session.getUserId());
        }

        // ==========================================
        // FEATURE A: Chatbot FAQ and options handler
        // ==========================================
        if ("service_bot".equals(toUserId) && assignedAgentId == null) {
            // Content Auditing Filter Check
            if (originalContent != null && !originalContent.trim().isEmpty()) {
                com.im.util.SensitiveWordFilter.FilterResult filterResult = com.im.util.SensitiveWordFilter.INSTANCE.filter(originalContent);
                if (filterResult.isHasSensitiveWord()) {
                    String policy = com.im.util.SensitiveWordFilter.INSTANCE.getPolicy();
                    if ("BLOCK".equalsIgnoreCase(policy)) {
                        System.out.println("Audit Blocked: Message contains sensitive words. Intercepting!");
                        AckPacket blockedAck = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
                        blockedAck.setStatus(5001);
                        blockedAck.setMessage("消息包含安全违规内容，已拦截发送");
                        ctx.channel().writeAndFlush(blockedAck);
                        return;
                    } else if ("MASK".equalsIgnoreCase(policy)) {
                        messageRequestPacket.setMessage(filterResult.getFilteredText());
                    }
                }
            }

            // Save user message to DB
            try {
                com.im.mapper.MessageMapper messageMapper = SpringContextHolder.getBean(com.im.mapper.MessageMapper.class);
                if (messageMapper != null) {
                    com.im.entity.Message message = new com.im.entity.Message();
                    message.setMsgId(messageRequestPacket.getMsgId());
                    message.setFromUserId(session.getUserId());
                    message.setToUserId("service_bot");
                    message.setContent(messageRequestPacket.getMessage());
                    message.setType(1); // 1: Single
                    message.setMsgType(messageRequestPacket.getMsgType() != null ? messageRequestPacket.getMsgType() : 1);
                    message.setStatus(0); // Sent
                    message.setCreateTime(new java.util.Date());
                    messageMapper.insert(message);
                }
            } catch (Exception ex) {
                System.err.println("Failed to persist user-to-bot message: " + ex.getMessage());
            }

            // Send ACK back to user
            if (messageRequestPacket.getMsgId() != null) {
                AckPacket ackPacket = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
                ctx.channel().writeAndFlush(ackPacket);
            }

            // Bot response processing
            String text = messageRequestPacket.getMessage();
            String botReplyJson = "";
            boolean triggerTransfer = false;

            if (text != null && text.contains("转人工")) {
                triggerTransfer = true;
            } else if ("1".equals(text) || (text != null && (text.contains("物流") || text.contains("发货")))) {
                botReplyJson = "{"
                    + "\"text\":\"关于【商品未发货与物流查询 📦】\\n由于当前正值大促期间，商家将会在下单后 24-48 小时内陆续为您安排发货。发货后您可以直接在“我的订单”中查看实时物流动态哦！\","
                    + "\"options\":["
                    + "{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"},"
                    + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
                    + "]"
                    + "}";
            } else if ("2".equals(text) || (text != null && (text.contains("退款") || text.contains("退换")))) {
                botReplyJson = "{"
                    + "\"text\":\"关于【退换货与退款服务 💸】\\n1. 未发货订单：您可以直接申请退款，系统将在 1-3 个工作日内原路退回。\\n2. 已收到货订单：请先确保商品完好且不影响二次销售，在订单页发起“退货退款”并上传寄回快递单号，我们将在收到货后自动为您退款。\","
                    + "\"options\":["
                    + "{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"},"
                    + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
                    + "]"
                    + "}";
            } else {
                botReplyJson = "{"
                    + "\"text\":\"您好！我是您的智能客服助手。请问有什么可以帮您？\","
                    + "\"options\":["
                    + "{\"label\":\"商品未发货与物流查询 📦\",\"value\":\"1\"},"
                    + "{\"label\":\"退换货与退款服务 💸\",\"value\":\"2\"},"
                    + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
                    + "]"
                    + "}";
            }

            if (triggerTransfer && deskSessionManager != null) {
                StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
                Map<String, String> agent = deskSessionManager.dispatchAgent(session.getUserId());
                if (agent != null) {
                    String agentId = agent.get("id");
                    String agentName = agent.get("name");

                    botReplyJson = "{\"text\":\"已成功为您接入人工坐席【" + agentName + "】，接下来您的消息将直接发送给客服专员。\"}";

                    if (redisTemplate != null) {
                        JSONObject routeObj = new JSONObject();
                        routeObj.put("command", Command.CUSTOM_DESK_RESPONSE);
                        routeObj.put("toUserId", agentId);
                        routeObj.put("action", "agent_connected");
                        routeObj.put("status", "success");
                        routeObj.put("agentId", agentId);
                        routeObj.put("agentName", agentName);
                        routeObj.put("msgType", 1);
                        routeObj.put("content", "【接入提示】客户【" + session.getUserName() + "】（" + session.getUserId() + "）已接入您的客服席位！请开始服务。");

                        redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, routeObj.toJSONString());
                    }
                } else {
                    botReplyJson = "{\"text\":\"抱歉，当前无在线客服坐席。您可以稍后再试，或者留下您的问题。\",\"options\":[{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"}]}";
                }
            }

            // Save Bot message to DB
            String botMsgId = "bot-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 6);
            try {
                com.im.mapper.MessageMapper messageMapper = SpringContextHolder.getBean(com.im.mapper.MessageMapper.class);
                if (messageMapper != null) {
                    com.im.entity.Message botMessage = new com.im.entity.Message();
                    botMessage.setMsgId(botMsgId);
                    botMessage.setFromUserId("service_bot");
                    botMessage.setToUserId(session.getUserId());
                    botMessage.setContent(botReplyJson);
                    botMessage.setType(1); // 1: Single
                    botMessage.setMsgType(6); // Interactive card
                    botMessage.setStatus(0);
                    botMessage.setCreateTime(new java.util.Date());
                    messageMapper.insert(botMessage);
                }
            } catch (Exception ex) {
                System.err.println("Failed to persist bot message: " + ex.getMessage());
            }

            // Send Bot message to local client
            ChannelGroup channelGroup = SessionUtil.getChannelGroup(session.getUserId());
            if (channelGroup != null && !channelGroup.isEmpty()) {
                MessageResponsePacket botResponse = new MessageResponsePacket();
                botResponse.setFromUserId("service_bot");
                botResponse.setFromUserName("智能客服");
                botResponse.setMessage(botReplyJson);
                botResponse.setMsgId(botMsgId);
                botResponse.setMsgType(6);
                channelGroup.writeAndFlush(botResponse);
            }
            return;
        }

        // ==========================================
        // FEATURE B: Proxy Tunnel Routing Logic
        // ==========================================
        String realToUserId = toUserId;
        String realFromUserId = session.getUserId();
        String realFromUserName = session.getUserName();

        String dbToUserId = toUserId;
        String dbFromUserId = session.getUserId();

        // 1. Customer sends message to service_bot, redirected to human agent
        if ("service_bot".equals(toUserId) && assignedAgentId != null) {
            realToUserId = assignedAgentId;
            dbToUserId = "service_bot";
        }

        // 2. Agent sends message to customer, masked as service_bot
        if (session.getUserId().startsWith("agent_")) {
            realFromUserId = "service_bot";
            dbFromUserId = "service_bot";
        }

        // Blacklist Pre-Intercepting Check (exclude bot and agent interactions)
        if (!"service_bot".equals(realToUserId) && !realFromUserId.startsWith("agent_")) {
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
            if (redisTemplate != null) {
                Boolean isBlacklisted = redisTemplate.opsForSet().isMember("im:user:blacklist:" + realToUserId, session.getUserId());
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    System.out.println("Blacklist Blocked: Sender [" + session.getUserId() + "] is blacklisted by Receiver [" + realToUserId + "]. Intercepting message [" + messageRequestPacket.getMsgId() + "]");
                    AckPacket blockedAck = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
                    blockedAck.setStatus(5002);
                    blockedAck.setMessage("消息已发出，但被对方拒收了");
                    ctx.channel().writeAndFlush(blockedAck);
                    return;
                }
            }
        }

        // Content Auditing Filter Check
        if (originalContent != null && !originalContent.trim().isEmpty()) {
            com.im.util.SensitiveWordFilter.FilterResult filterResult = com.im.util.SensitiveWordFilter.INSTANCE.filter(originalContent);
            if (filterResult.isHasSensitiveWord()) {
                String policy = com.im.util.SensitiveWordFilter.INSTANCE.getPolicy();
                if ("BLOCK".equalsIgnoreCase(policy)) {
                    System.out.println("Audit Blocked: Message " + messageRequestPacket.getMsgId() + " contains sensitive words. Intercepting!");
                    AckPacket blockedAck = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
                    blockedAck.setStatus(5001);
                    blockedAck.setMessage("消息包含安全违规内容，已拦截发送");
                    ctx.channel().writeAndFlush(blockedAck);
                    return;
                } else if ("MASK".equalsIgnoreCase(policy)) {
                    messageRequestPacket.setMessage(filterResult.getFilteredText());
                }
            }
        }

        // Persist message in MySQL
        try {
            com.im.mapper.MessageMapper messageMapper = SpringContextHolder.getBean(com.im.mapper.MessageMapper.class);
            if (messageMapper != null) {
                com.im.entity.Message message = new com.im.entity.Message();
                message.setMsgId(messageRequestPacket.getMsgId());
                message.setFromUserId(dbFromUserId);
                message.setToUserId(dbToUserId);
                message.setContent(messageRequestPacket.getMessage());
                message.setType(1); // 1: Single
                message.setMsgType(messageRequestPacket.getMsgType() != null ? messageRequestPacket.getMsgType() : 1);
                message.setStatus(0); // Sent
                message.setCreateTime(new java.util.Date());
                message.setQuoteMsgId(messageRequestPacket.getQuoteMsgId());
                message.setQuoteSender(messageRequestPacket.getQuoteSender());
                message.setQuoteContent(messageRequestPacket.getQuoteContent());
                messageMapper.insert(message);
            }
        } catch (Exception ex) {
            System.err.println("Failed to persist WebSocket message: " + ex.getMessage());
        }

        // Send ACK back to sender
        if (messageRequestPacket.getMsgId() != null) {
            AckPacket ackPacket = new AckPacket(messageRequestPacket.getMsgId(), "SERVER");
            ctx.channel().writeAndFlush(ackPacket);
        }

        // Construct route payload
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("toUserId", realToUserId);
        jsonPayload.put("fromUserId", realFromUserId);
        jsonPayload.put("fromUserName", realFromUserName);
        jsonPayload.put("message", messageRequestPacket.getMessage());
        jsonPayload.put("msgId", messageRequestPacket.getMsgId());
        jsonPayload.put("msgType", messageRequestPacket.getMsgType() != null ? messageRequestPacket.getMsgType() : 1);
        jsonPayload.put("quoteMsgId", messageRequestPacket.getQuoteMsgId());
        jsonPayload.put("quoteSender", messageRequestPacket.getQuoteSender());
        jsonPayload.put("quoteContent", messageRequestPacket.getQuoteContent());

        try {
            RedisService redisService = SpringContextHolder.getBean(RedisService.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            String globalSession = redisService != null ? redisService.getSession(realToUserId) : null;

            if (globalSession != null) {
                if (redisTemplate != null) {
                    redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, jsonPayload.toJSONString());
                }
            } else {
                if (redisTemplate != null) {
                    String offlineKey = "im:offline:messages:" + realToUserId;
                    redisTemplate.opsForList().rightPush(offlineKey, jsonPayload.toJSONString());
                }

                // Invoke TIMPushService for offline push
                try {
                    TIMPushService timPushService = SpringContextHolder.getBean(TIMPushService.class);
                    if (timPushService != null) {
                        timPushService.pushOffline(realFromUserId, realToUserId, realFromUserName, messageRequestPacket.getMessage());
                    }
                } catch (Exception pe) {
                    System.err.println("Failed to dispatch offline push: " + pe.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Redis services failed. Falling back to local delivery:");
            e.printStackTrace();

            ChannelGroup channelGroup = SessionUtil.getChannelGroup(realToUserId);
            if (channelGroup != null && !channelGroup.isEmpty()) {
                MessageResponsePacket messageResponsePacket = new MessageResponsePacket();
                messageResponsePacket.setFromUserId(realFromUserId);
                messageResponsePacket.setFromUserName(realFromUserName);
                messageResponsePacket.setMessage(messageRequestPacket.getMessage());
                messageResponsePacket.setMsgId(messageRequestPacket.getMsgId());
                messageResponsePacket.setMsgType(messageRequestPacket.getMsgType());
                messageResponsePacket.setQuoteMsgId(messageRequestPacket.getQuoteMsgId());
                messageResponsePacket.setQuoteSender(messageRequestPacket.getQuoteSender());
                messageResponsePacket.setQuoteContent(messageRequestPacket.getQuoteContent());
                channelGroup.writeAndFlush(messageResponsePacket);
            }
        }
    }
}
