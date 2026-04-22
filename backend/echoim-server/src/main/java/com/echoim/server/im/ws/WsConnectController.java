package com.echoim.server.im.ws;

import com.echoim.server.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/im")
public class WsConnectController {

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(Map.of(
                "mode", "single-server",
                "transport", "websocket-pending",
                "status", "skeleton-ready"
        ));
    }
}
