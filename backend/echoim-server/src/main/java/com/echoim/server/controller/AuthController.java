package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.service.auth.AuthService;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.RegisterResponseVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponseVo> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        return ApiResponse.success(authService.register(requestDto));
    }

    @PostMapping("/login")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-login", permits = 10, windowSeconds = 60, message = "登录过于频繁")
    public ApiResponse<LoginResponseVo> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ApiResponse.success(authService.login(requestDto));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @PostMapping("/change-password")
    @RequireLogin
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(LoginUserContext.requireUserId(), request.oldPassword(), request.newPassword());
        return ApiResponse.success();
    }

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank(message = "旧密码不能为空") String oldPassword,
            @jakarta.validation.constraints.NotBlank(message = "新密码不能为空") String newPassword
    ) {
    }
}
