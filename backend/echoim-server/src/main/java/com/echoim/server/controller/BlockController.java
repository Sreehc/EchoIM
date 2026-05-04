package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.block.BlockService;
import com.echoim.server.vo.block.BlockedUserItemVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @RequireLogin
    @PostMapping("/{targetUserId}")
    public ApiResponse<Void> block(@PathVariable Long targetUserId) {
        blockService.blockUser(LoginUserContext.requireUserId(), targetUserId);
        return ApiResponse.success();
    }

    @RequireLogin
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unblock(@PathVariable Long targetUserId) {
        blockService.unblockUser(LoginUserContext.requireUserId(), targetUserId);
        return ApiResponse.success();
    }

    @RequireLogin
    @GetMapping
    public ApiResponse<List<BlockedUserItemVo>> list() {
        return ApiResponse.success(blockService.listBlockedUsers(LoginUserContext.requireUserId()));
    }
}
