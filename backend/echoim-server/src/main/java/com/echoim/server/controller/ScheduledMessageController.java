package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.dto.message.CreateScheduledMessageRequestDto;
import com.echoim.server.service.message.ScheduledMessageService;
import com.echoim.server.vo.message.ScheduledMessageItemVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduled-messages")
@RequireLogin
public class ScheduledMessageController {

    private final ScheduledMessageService scheduledMessageService;

    public ScheduledMessageController(ScheduledMessageService scheduledMessageService) {
        this.scheduledMessageService = scheduledMessageService;
    }

    @PostMapping
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "scheduled-message-create", permits = 20, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<ScheduledMessageItemVo> createScheduledMessage(@Valid @RequestBody CreateScheduledMessageRequestDto request) {
        return ApiResponse.success(scheduledMessageService.createScheduledMessage(LoginUserContext.requireUserId(), request));
    }

    @GetMapping
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "scheduled-message-list", permits = 30, windowSeconds = 60, message = "查询过于频繁")
    public ApiResponse<List<ScheduledMessageItemVo>> listScheduledMessages(@RequestParam Long conversationId) {
        return ApiResponse.success(scheduledMessageService.listScheduledMessages(LoginUserContext.requireUserId(), conversationId));
    }

    @PutMapping("/{id}/cancel")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "scheduled-message-cancel", permits = 20, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<Void> cancelScheduledMessage(@PathVariable Long id) {
        scheduledMessageService.cancelScheduledMessage(LoginUserContext.requireUserId(), id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/send-now")
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "scheduled-message-send-now", permits = 20, windowSeconds = 60, message = "操作过于频繁")
    public ApiResponse<Void> sendScheduledMessageImmediately(@PathVariable Long id) {
        scheduledMessageService.sendScheduledMessageImmediately(LoginUserContext.requireUserId(), id);
        return ApiResponse.success();
    }
}
