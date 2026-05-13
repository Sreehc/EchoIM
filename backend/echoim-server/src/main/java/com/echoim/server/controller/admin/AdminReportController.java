package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.admin.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequireLogin
@RequireAdmin
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listReports(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(reportService.listReports(status, pageNo, pageSize));
    }

    @PutMapping("/{id}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long id,
                                           @RequestBody Map<String, Object> request) {
        Integer action = (Integer) request.get("action");
        String remark = (String) request.get("remark");
        reportService.handleReport(id, LoginUserContext.requireUserId(), action, remark);
        return ApiResponse.success();
    }
}
