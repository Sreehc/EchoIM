package com.echoim.server.im.ws;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.config.ImProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/im")
public class WsConnectController {

    private final ImProperties imProperties;

    public WsConnectController(ImProperties imProperties) {
        this.imProperties = imProperties;
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
}
