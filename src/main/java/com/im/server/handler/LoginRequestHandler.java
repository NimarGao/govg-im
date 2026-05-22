package com.im.server.handler;

import com.im.protocol.command.LoginRequestPacket;
import com.im.protocol.command.LoginResponsePacket;
import com.im.util.Session;
import com.im.util.SessionUtil;
import com.im.util.SpringContextHolder;
import com.im.util.redis.RedisService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.util.UUID;

@ChannelHandler.Sharable
public class LoginRequestHandler extends SimpleChannelInboundHandler<LoginRequestPacket> {

    public static final LoginRequestHandler INSTANCE = new LoginRequestHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestPacket loginRequestPacket) {
        LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
        loginResponsePacket.setVersion(loginRequestPacket.getVersion());

        String userId = null;
        String username = null;
        boolean loginSuccess = false;
        String failReason = null;

        // 1. Verify token if provided
        if (loginRequestPacket.getToken() != null && !loginRequestPacket.getToken().trim().isEmpty()) {
            try {
                StringRedisTemplate redisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
                String tokenKey = "im:token:" + loginRequestPacket.getToken().trim();
                String tokenVal = redisTemplate.opsForValue().get(tokenKey);

                if (tokenVal != null && tokenVal.contains(":")) {
                    String[] parts = tokenVal.split(":", 2);
                    userId = parts[0];
                    username = parts[1];
                    loginSuccess = true;
                    
                    // Keep token valid for its TTL (24h) to support robust reconnection and retries
                    // redisTemplate.delete(tokenKey);
                } else {
                    failReason = "Invalid or expired login token";
                }
            } catch (Exception e) {
                System.err.println("Error verifying login token in Redis: " + e.getMessage());
                failReason = "Auth service unavailable";
            }
        } 
        // 2. Fallback to mock authentication for legacy compatibility
        else {
            if (loginRequestPacket.getUsername() != null && !loginRequestPacket.getUsername().trim().isEmpty()) {
                username = loginRequestPacket.getUsername().trim();
                userId = loginRequestPacket.getUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    userId = UUID.nameUUIDFromBytes(username.getBytes()).toString();
                }
                loginSuccess = true;
            } else {
                failReason = "Username is required for legacy login";
            }
        }

        // 3. Process login outcome
        if (loginSuccess) {
            loginResponsePacket.setSuccess(true);
            loginResponsePacket.setUserId(userId);
            loginResponsePacket.setUsername(username);

            // Bind session locally in Netty memory
            SessionUtil.bindSession(new Session(userId, username), ctx.channel());
            System.out.println("User [" + username + "] (ID: " + userId + ") logged in successfully.");

            // Bind session globally in Redis (for clustered routing)
            try {
                RedisService redisService = SpringContextHolder.getBean(RedisService.class);
                String ip = InetAddress.getLocalHost().getHostAddress();
                // We default to 10086 as TCP server port (Session will be mapped here)
                redisService.saveSession(userId, ip, 10086);
            } catch (Exception e) {
                System.err.println("Failed to save global session to Redis: " + e.getMessage());
            }
        } else {
            loginResponsePacket.setSuccess(false);
            loginResponsePacket.setReason(failReason);
            System.out.println("Login failed: " + failReason);
        }

        // Write response back to client
        ctx.channel().writeAndFlush(loginResponsePacket);
    }
}
