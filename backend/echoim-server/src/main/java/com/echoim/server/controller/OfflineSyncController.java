package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.offline.OfflineSyncRequestDto;
import com.echoim.server.im.service.OfflineSyncService;
import com.echoim.server.vo.offline.OfflineSyncResponseVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offline-sync")
public class OfflineSyncController {

    private final OfflineSyncService offlineSyncService;

    public OfflineSyncController(OfflineSyncService offlineSyncService) {
        this.offlineSyncService = offlineSyncService;
    }

    @RequireLogin
    @PostMapping("/messages")
    public ApiResponse<OfflineSyncResponseVo> syncMessages(@Valid @RequestBody OfflineSyncRequestDto requestDto) {
        return ApiResponse.success(offlineSyncService.syncMessages(LoginUserContext.requireUserId(), requestDto));
    }
}
