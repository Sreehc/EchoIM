package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.friend.CreateFriendRequestDto;
import com.echoim.server.service.friend.FriendRequestService;
import com.echoim.server.vo.friend.FriendRequestCreateVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FriendController {

    private final FriendRequestService friendRequestService;

    public FriendController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    @GetMapping("/friends")
    public ApiResponse<List<Map<String, Object>>> friends() {
        return ApiResponse.success(List.of(
                Map.of(
                        "friendUserId", 10002L,
                        "nickname", "Echo用户02",
                        "remark", "产品同学",
                        "avatarUrl", ""
                )
        ));
    }

    @RequireLogin
    @PostMapping("/friend-requests")
    public ApiResponse<FriendRequestCreateVo> createRequest(@Valid @RequestBody CreateFriendRequestDto requestDto) {
        return ApiResponse.success(friendRequestService.create(LoginUserContext.requireUserId(), requestDto));
    }

    @GetMapping("/friend-requests")
    public ApiResponse<List<Map<String, Object>>> friendRequests() {
        return ApiResponse.success(List.of(
                Map.of(
                        "requestId", 1L,
                        "fromUserId", 10001L,
                        "toUserId", 10002L,
                        "applyMsg", "你好，想加你为好友",
                        "status", 1,
                        "createdAt", LocalDateTime.now().toString()
                )
        ));
    }

    @PutMapping("/friend-requests/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@PathVariable Long id) {
        return ApiResponse.success(Map.of("requestId", id, "status", 1));
    }

    @PutMapping("/friend-requests/{id}/reject")
    public ApiResponse<Map<String, Object>> reject(@PathVariable Long id) {
        return ApiResponse.success(Map.of("requestId", id, "status", 2));
    }

    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> delete(@PathVariable Long friendId) {
        return ApiResponse.success();
    }
}
