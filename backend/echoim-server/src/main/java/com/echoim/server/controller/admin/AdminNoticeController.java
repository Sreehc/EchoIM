package com.echoim.server.controller.admin;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireAdmin;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.service.notice.SystemNoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequireLogin
@RequireAdmin
@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final SystemNoticeService systemNoticeService;

    public AdminNoticeController(SystemNoticeService systemNoticeService) {
        this.systemNoticeService = systemNoticeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listNotices(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(systemNoticeService.pageAdminNotices(status, pageNo, pageSize));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createNotice(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(systemNoticeService.createNotice(request));
    }

    @PutMapping("/{id}/withdraw")
    public ApiResponse<Void> withdrawNotice(@PathVariable Long id) {
        systemNoticeService.withdrawNotice(id);
        return ApiResponse.success();
    }
}
