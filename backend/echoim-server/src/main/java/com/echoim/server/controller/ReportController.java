package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.service.admin.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequireLogin
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "report-submit", permits = 5, windowSeconds = 300, message = "举报过于频繁")
    public ApiResponse<Void> submitReport(@RequestBody Map<String, Object> request) {
        Integer targetType = (Integer) request.get("targetType");
        Long targetId = request.get("targetId") instanceof Number ? ((Number) request.get("targetId")).longValue() : null;
        String reason = (String) request.get("reason");
        String description = (String) request.get("description");
        reportService.submitReport(LoginUserContext.requireUserId(), targetType, targetId, reason, description);
        return ApiResponse.success();
    }
}
