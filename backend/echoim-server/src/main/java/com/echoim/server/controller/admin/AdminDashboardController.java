package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.service.admin.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RequireLogin
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(dashboardService.getOverview());
    }

    @GetMapping("/message-trend")
    public ApiResponse<List<Map<String, Object>>> messageTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboardService.getMessageTrend(days));
    }

    @GetMapping("/user-trend")
    public ApiResponse<List<Map<String, Object>>> userTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboardService.getUserTrend(days));
    }

    @GetMapping("/message-types")
    public ApiResponse<List<Map<String, Object>>> messageTypes() {
        return ApiResponse.success(dashboardService.getMessageTypeBreakdown());
    }

    @GetMapping("/online-stats")
    public ApiResponse<Map<String, Object>> onlineStats() {
        return ApiResponse.success(dashboardService.getOnlineStats());
    }
}
