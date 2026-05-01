package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.im.session.ImSessionManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;
    private final ImSessionManager imSessionManager;

    public HealthController(DataSource dataSource,
                            StringRedisTemplate stringRedisTemplate,
                            ImSessionManager imSessionManager) {
        this.dataSource = dataSource;
        this.stringRedisTemplate = stringRedisTemplate;
        this.imSessionManager = imSessionManager;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allHealthy = true;

        // Database check
        try (Connection conn = dataSource.getConnection()) {
            checks.put("database", Map.of("status", "UP", "validationQuery", conn.isValid(2)));
        } catch (Exception ex) {
            checks.put("database", Map.of("status", "DOWN", "error", ex.getMessage()));
            allHealthy = false;
        }

        // Redis check
        try {
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            checks.put("redis", Map.of("status", "UP", "ping", pong));
        } catch (Exception ex) {
            checks.put("redis", Map.of("status", "DOWN", "error", ex.getMessage()));
            allHealthy = false;
        }

        // WebSocket sessions
        int onlineCount = imSessionManager.allSessions().size();
        checks.put("websocket", Map.of("status", "UP", "onlineConnections", onlineCount));

        checks.put("service", "echoim-server");
        checks.put("time", LocalDateTime.now().toString());
        checks.put("status", allHealthy ? "UP" : "DEGRADED");

        return ApiResponse.success(checks);
    }
}
