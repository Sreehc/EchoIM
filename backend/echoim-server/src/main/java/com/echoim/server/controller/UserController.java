package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserProfileVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/search")
    public ApiResponse<PageResponse<Map<String, Object>>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo 最小为 1") long pageNo,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "pageSize 最小为 1") long pageSize
    ) {
        List<Map<String, Object>> list = List.of(Map.of(
                "userId", 10002L,
                "userNo", "E10002",
                "nickname", "Echo用户02",
                "avatarUrl", "",
                "friendStatus", 0,
                "keyword", keyword
        ));
        return ApiResponse.success(new PageResponse<>(list, pageNo, pageSize, list.size()));
    }
}
