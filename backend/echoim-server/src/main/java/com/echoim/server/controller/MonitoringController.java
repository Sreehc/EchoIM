package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.im.monitor.WsMetrics;
import com.echoim.server.im.session.ImSessionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitoringController {

    private final ImSessionManager imSessionManager;
    private final WsMetrics wsMetrics;

    public MonitoringController(ImSessionManager imSessionManager, WsMetrics wsMetrics) {
        this.imSessionManager = imSessionManager;
        this.wsMetrics = wsMetrics;
    }

    @GetMapping("/ws")
    public ApiResponse<Map<String, Object>> wsMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("onlineConnections", imSessionManager.allSessions().size());
        metrics.putAll(wsMetrics.snapshot());
        return ApiResponse.success(metrics);
    }

    @GetMapping("/jvm")
    public ApiResponse<Map<String, Object>> jvmMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        metrics.put("uptimeMs", runtime.getUptime());
        metrics.put("heapUsedBytes", memory.getHeapMemoryUsage().getUsed());
        metrics.put("heapMaxBytes", memory.getHeapMemoryUsage().getMax());
        metrics.put("nonHeapUsedBytes", memory.getNonHeapMemoryUsage().getUsed());
        metrics.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        metrics.put("threadCount", Thread.activeCount());

        return ApiResponse.success(metrics);
    }
}
