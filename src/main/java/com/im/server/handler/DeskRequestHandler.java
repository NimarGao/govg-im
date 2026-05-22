package com.im.server.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.im.config.RedisPubSubConfig;
import com.im.protocol.command.Command;
import com.im.protocol.command.DeskRequestPacket;
import com.im.protocol.command.DeskResponsePacket;
import com.im.server.service.DeskSessionManager;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class DeskRequestHandler extends SimpleChannelInboundHandler<DeskRequestPacket> {

    public static final DeskRequestHandler INSTANCE = new DeskRequestHandler();

    private DeskRequestHandler() {
    }

    private static final String WELCOME_JSON = "{"
            + "\"text\":\"您好！我是您的智能客服助手。请问有什么可以帮您？\","
            + "\"options\":["
            + "{\"label\":\"商品未发货与物流查询 📦\",\"value\":\"1\"},"
            + "{\"label\":\"退换货与退款服务 💸\",\"value\":\"2\"},"
            + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
            + "]"
            + "}";

    private static final String OPTION1_JSON = "{"
            + "\"text\":\"关于【商品未发货与物流查询 📦】\\n由于当前正值大促期间，商家将会在下单后 24-48 小时内陆续为您安排发货。发货后您可以直接在“我的订单”中查看实时物流动态哦！\","
            + "\"options\":["
            + "{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"},"
            + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
            + "]"
            + "}";

    private static final String OPTION2_JSON = "{"
            + "\"text\":\"关于【退换货与退款服务 💸】\\n1. 未发货订单：您可以直接申请退款，系统将在 1-3 个工作日内原路退回。\\n2. 已收到货订单：请先确保商品完好且不影响二次销售，在订单页发起“退货退款”并上传寄回快递单号，我们将在收到货后自动为您退款。\","
            + "\"options\":["
            + "{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"},"
            + "{\"label\":\"转人工客服 🧑‍💼\",\"value\":\"transfer_agent\"}"
            + "]"
            + "}";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DeskRequestPacket packet) throws Exception {
        Session session = SessionUtil.getSession(ctx.channel());
        if (session == null) {
            return;
        }

        String userId = session.getUserId();
        String action = packet.getAction();

        System.out.println("DeskRequest: User [" + userId + "] triggered action [" + action + "]");

        if ("get_welcome".equalsIgnoreCase(action)) {
            DeskResponsePacket response = new DeskResponsePacket(
                "get_welcome",
                "success",
                null,
                null,
                6,
                WELCOME_JSON
            );
            ctx.channel().writeAndFlush(response);

        } else if ("ask_robot".equalsIgnoreCase(action)) {
            String robotMsg = packet.getRobotMsg();
            String responseContent = WELCOME_JSON;

            if ("1".equals(robotMsg) || (robotMsg != null && (robotMsg.contains("物流") || robotMsg.contains("发货")))) {
                responseContent = OPTION1_JSON;
            } else if ("2".equals(robotMsg) || (robotMsg != null && (robotMsg.contains("退款") || robotMsg.contains("退换")))) {
                responseContent = OPTION2_JSON;
            }

            DeskResponsePacket response = new DeskResponsePacket(
                "ask_robot",
                "success",
                null,
                null,
                6,
                responseContent
            );
            ctx.channel().writeAndFlush(response);

        } else if ("transfer_agent".equalsIgnoreCase(action)) {
            DeskSessionManager deskSessionManager = SpringContextHolder.getBean(DeskSessionManager.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            if (deskSessionManager != null) {
                Map<String, String> agent = deskSessionManager.dispatchAgent(userId);
                if (agent != null) {
                    String agentId = agent.get("id");
                    String agentName = agent.get("name");

                    // 1. Reply to user
                    DeskResponsePacket response = new DeskResponsePacket(
                        "transfer_agent",
                        "success",
                        agentId,
                        agentName,
                        6,
                        "{\"text\":\"已成功为您接入人工坐席【" + agentName + "】，接下来您的消息将直接发送给客服专员。\"}"
                    );
                    ctx.channel().writeAndFlush(response);

                    // 2. Dispatch connection route signal to agent via Redis
                    if (redisTemplate != null) {
                        JSONObject routeObj = new JSONObject();
                        routeObj.put("command", Command.CUSTOM_DESK_RESPONSE);
                        routeObj.put("toUserId", agentId);
                        routeObj.put("action", "agent_connected");
                        routeObj.put("status", "success");
                        routeObj.put("agentId", agentId);
                        routeObj.put("agentName", agentName);
                        routeObj.put("msgType", 1);
                        routeObj.put("content", "【接入提示】客户【" + session.getUserName() + "】（" + userId + "）已接入您的客服席位！请开始服务。");

                        redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, routeObj.toJSONString());
                    }
                } else {
                    // No agent online
                    DeskResponsePacket response = new DeskResponsePacket(
                        "transfer_agent",
                        "failed",
                        null,
                        null,
                        6,
                        "{\"text\":\"抱歉，当前无在线客服坐席。您可以稍后再试，或者留下您的问题。\",\"options\":[{\"label\":\"返回主菜单 ↩️\",\"value\":\"menu\"}]}"
                    );
                    ctx.channel().writeAndFlush(response);
                }
            }

        } else if ("submit_rate".equalsIgnoreCase(action)) {
            Integer score = packet.getScore();
            String feedback = packet.getFeedback();
            if (score == null) {
                score = 5;
            }

            DeskSessionManager deskSessionManager = SpringContextHolder.getBean(DeskSessionManager.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
            JdbcTemplate jdbcTemplate = SpringContextHolder.getBean(JdbcTemplate.class);

            String agentId = null;
            if (deskSessionManager != null) {
                agentId = deskSessionManager.getAssignedAgent(userId);
            }
            if (agentId == null && redisTemplate != null) {
                agentId = redisTemplate.opsForValue().get("im:desk:last_agent:" + userId);
            }
            if (agentId == null) {
                agentId = "agent_001";
            }

            // Save rating in MySQL
            if (jdbcTemplate != null) {
                try {
                    jdbcTemplate.update(
                        "INSERT INTO im_desk_rating (user_id, agent_id, score, feedback, create_time) VALUES (?, ?, ?, ?, ?)",
                        userId, agentId, score, feedback, new Date()
                    );
                    System.out.println("DeskRequestHandler: Successfully logged evaluation rating for agent: " + agentId);
                } catch (Exception e) {
                    System.err.println("DeskRequestHandler: Failed to persist rating in DB: " + e.getMessage());
                }
            }

            // Clear session and last_agent
            if (deskSessionManager != null) {
                deskSessionManager.terminateSession(userId);
            }
            if (redisTemplate != null) {
                redisTemplate.delete("im:desk:last_agent:" + userId);
            }

            // Send confirmation card to user
            DeskResponsePacket response = new DeskResponsePacket(
                "submit_rate",
                "success",
                agentId,
                null,
                6,
                "{\"text\":\"感谢您的评价，祝您生活愉快！❤️\"}"
            );
            ctx.channel().writeAndFlush(response);

        } else if ("close_session".equalsIgnoreCase(action)) {
            String customerId = packet.getUserId();
            DeskSessionManager deskSessionManager = SpringContextHolder.getBean(DeskSessionManager.class);
            StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);

            if (deskSessionManager != null && customerId != null) {
                String assignedAgentId = deskSessionManager.getAssignedAgent(customerId);
                if (assignedAgentId != null) {
                    if (redisTemplate != null) {
                        redisTemplate.opsForValue().set("im:desk:last_agent:" + customerId, assignedAgentId, 30, TimeUnit.MINUTES);
                    }

                    // Push rating request to customer
                    if (redisTemplate != null) {
                        JSONObject routeObj = new JSONObject();
                        routeObj.put("command", Command.CUSTOM_DESK_RESPONSE);
                        routeObj.put("toUserId", customerId);
                        routeObj.put("action", "show_rating");
                        routeObj.put("status", "success");
                        routeObj.put("agentId", assignedAgentId);
                        routeObj.put("msgType", 6);
                        routeObj.put("content", "{\"text\":\"人工服务已结束。请对本次服务进行打分评价哦 🌟\",\"isRatingCard\":true}");

                        redisTemplate.convertAndSend(RedisPubSubConfig.ROUTE_CHANNEL, routeObj.toJSONString());
                    }

                    // Release session mapping
                    deskSessionManager.terminateSession(customerId);
                }
            }

            // Send end confirmation message to agent
            DeskResponsePacket agentConfirm = new DeskResponsePacket(
                "close_session",
                "success",
                session.getUserId(),
                session.getUserName(),
                1,
                "【系统提示】会话已结束。"
            );
            ctx.channel().writeAndFlush(agentConfirm);
        }
    }
}
