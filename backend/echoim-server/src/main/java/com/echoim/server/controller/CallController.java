package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.dto.call.CreateCallRequestDto;
import com.echoim.server.service.call.CallService;
import com.echoim.server.vo.call.CallSessionSummaryVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calls")
@RequireLogin
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "call-create", permits = 20, windowSeconds = 60, message = "发起通话过于频繁")
    public ApiResponse<CallSessionSummaryVo> create(@Valid @RequestBody CreateCallRequestDto requestDto) {
        return ApiResponse.success(callService.createCall(
                LoginUserContext.requireUserId(),
                requestDto.getConversationId(),
                requestDto.getCallType()
        ));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<CallSessionSummaryVo> accept(@PathVariable Long id) {
        return ApiResponse.success(callService.acceptCall(LoginUserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<CallSessionSummaryVo> reject(@PathVariable Long id) {
        return ApiResponse.success(callService.rejectCall(LoginUserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CallSessionSummaryVo> cancel(@PathVariable Long id) {
        return ApiResponse.success(callService.cancelCall(LoginUserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<CallSessionSummaryVo> end(@PathVariable Long id) {
        return ApiResponse.success(callService.endCall(LoginUserContext.requireUserId(), id));
    }

    @GetMapping("/{id}")
    public ApiResponse<CallSessionSummaryVo> get(@PathVariable Long id) {
        return ApiResponse.success(callService.getCall(LoginUserContext.requireUserId(), id));
    }
}
