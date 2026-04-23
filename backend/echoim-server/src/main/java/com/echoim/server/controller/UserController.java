package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserPublicProfileVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.echoim.server.vo.user.UserSearchItemVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @RequireLogin
    @GetMapping("/me")
    public ApiResponse<UserProfileVo> me() {
        return ApiResponse.success(userProfileService.getCurrentProfile(LoginUserContext.requireUserId()));
    }

    @RequireLogin
    @PutMapping("/me")
    public ApiResponse<UserProfileVo> updateMe(@Valid @RequestBody UpdateProfileRequestDto requestDto) {
        return ApiResponse.success(userProfileService.updateCurrentProfile(LoginUserContext.requireUserId(), requestDto));
    }

    @RequireLogin
    @GetMapping("/search")
    public ApiResponse<PageResponse<UserSearchItemVo>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo 最小为 1") long pageNo,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "pageSize 最小为 1") long pageSize
    ) {
        return ApiResponse.success(userProfileService.searchUsers(LoginUserContext.requireUserId(), keyword, pageNo, pageSize));
    }

    @RequireLogin
    @GetMapping("/{id}")
    public ApiResponse<UserPublicProfileVo> profile(@PathVariable Long id) {
        return ApiResponse.success(userProfileService.getPublicProfile(LoginUserContext.requireUserId(), id));
    }
}
