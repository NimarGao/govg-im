package com.im.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${im.external.api-key:your-default-secure-key}")
    private String apiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Preflight requests should bypass
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        String prefix = "Bearer ";

        if (authHeader != null && authHeader.startsWith(prefix)) {
            String clientKey = authHeader.substring(prefix.length()).trim();
            if (apiKey.equals(clientKey)) {
                return true;
            }
        }

        // Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("{\"success\":false,\"message\":\"Unauthorized: Invalid or missing API Key\"}");
            writer.flush();
        }
        return false;
    }
}
