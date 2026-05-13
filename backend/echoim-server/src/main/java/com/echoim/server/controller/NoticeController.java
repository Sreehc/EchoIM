package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.notice.SystemNoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@RequireLogin
public class NoticeController {

    private final SystemNoticeService systemNoticeService;

    public NoticeController(SystemNoticeService systemNoticeService) {
        this.systemNoticeService = systemNoticeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listNotices(@RequestParam(defaultValue = "1") long pageNo,
                                                        @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(systemNoticeService.pageUserNotices(LoginUserContext.requireUserId(), pageNo, pageSize));
    }

    @PutMapping("/{noticeId}/read")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable Long noticeId) {
        return ApiResponse.success(systemNoticeService.markNoticeRead(LoginUserContext.requireUserId(), noticeId));
    }
}
