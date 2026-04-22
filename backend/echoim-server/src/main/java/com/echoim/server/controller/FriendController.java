package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.friend.CreateFriendRequestDto;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.service.friend.FriendRequestService;
import com.echoim.server.vo.friend.FriendItemVo;
import com.echoim.server.vo.friend.FriendRequestCreateVo;
import com.echoim.server.vo.friend.FriendRequestItemVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FriendController {

    private final FriendService friendService;
    private final FriendRequestService friendRequestService;

    public FriendController(FriendService friendService, FriendRequestService friendRequestService) {
        this.friendService = friendService;
        this.friendRequestService = friendRequestService;
    }

    @RequireLogin
    @GetMapping("/friends")
    public ApiResponse<List<FriendItemVo>> friends() {
        return ApiResponse.success(friendService.listFriends(LoginUserContext.requireUserId()));
    }

    @RequireLogin
    @PostMapping("/friend-requests")
    public ApiResponse<FriendRequestCreateVo> createRequest(@Valid @RequestBody CreateFriendRequestDto requestDto) {
        return ApiResponse.success(friendRequestService.create(LoginUserContext.requireUserId(), requestDto));
    }

    @RequireLogin
    @GetMapping("/friend-requests")
    public ApiResponse<List<FriendRequestItemVo>> friendRequests() {
        return ApiResponse.success(friendService.listRelatedRequests(LoginUserContext.requireUserId()));
    }

    @RequireLogin
    @PutMapping("/friend-requests/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        friendService.approveRequest(LoginUserContext.requireUserId(), id);
        return ApiResponse.success();
    }

    @RequireLogin
    @PutMapping("/friend-requests/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        friendService.rejectRequest(LoginUserContext.requireUserId(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<Void> delete(@PathVariable Long friendId) {
        return ApiResponse.success();
    }
}
