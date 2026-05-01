package com.echoim.server.im.ws;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.config.ImProperties;
import com.echoim.server.im.service.ImOnlineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/im")
public class WsConnectController {

    private final ImProperties imProperties;
    private final ImOnlineService imOnlineService;

    public WsConnectController(ImProperties imProperties, ImOnlineService imOnlineService) {
        this.imProperties = imProperties;
        this.imOnlineService = imOnlineService;
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(Map.of(
                "mode", "single-server",
                "transport", "netty-websocket",
                "status", "ready",
                "port", imProperties.getPort(),
                "path", imProperties.getWsPath()
        ));
    }

    @RequireLogin
    @GetMapping("/online-status")
    public ApiResponse<Map<Long, Boolean>> onlineStatus(@RequestParam List<Long> userIds) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long userId : userIds) {
            result.put(userId, imOnlineService.isOnline(userId));
        }
        return ApiResponse.success(result);
    }
}
