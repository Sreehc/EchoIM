package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserPublicPageVo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/public/users")
public class PublicUserController {

    private final UserProfileService userProfileService;

    public PublicUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/by-username/{username}")
    public ApiResponse<UserPublicPageVo> profileByUsername(@PathVariable @NotBlank String username) {
        return ApiResponse.success(userProfileService.getPublicPageByUsername(username));
    }
}
