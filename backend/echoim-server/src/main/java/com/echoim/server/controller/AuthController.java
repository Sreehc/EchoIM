package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.service.auth.AuthService;
import com.echoim.server.vo.auth.CodeDispatchVo;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.RecoveryVerifyVo;
import com.echoim.server.vo.auth.RegisterResponseVo;
import com.echoim.server.vo.auth.SecurityEventItemVo;
import com.echoim.server.vo.auth.TrustedDeviceItemVo;
import com.echoim.server.vo.user.UserProfileVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ApiResponse<LoginResponseVo> login(@Valid @RequestBody LoginRequestDto requestDto, HttpServletRequest request) {
        return ApiResponse.success(authService.login(requestDto, resolveIp(request), resolveUserAgent(request)));
    }

    @PostMapping("/login/challenge/verify")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-login-verify", permits = 20, windowSeconds = 60, message = "验证过于频繁")
    public ApiResponse<LoginResponseVo> verifyLoginChallenge(@Valid @RequestBody LoginChallengeVerifyRequest request,
                                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.verifyLoginChallenge(
                request.challengeTicket(),
                request.code(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/login/challenge/resend")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-login-resend", permits = 10, windowSeconds = 60, message = "发送过于频繁")
    public ApiResponse<CodeDispatchVo> resendLoginChallenge(@Valid @RequestBody LoginChallengeResendRequest request,
                                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.resendLoginChallenge(
                request.challengeTicket(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/trusted-devices/login")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-device-login", permits = 20, windowSeconds = 60, message = "切换过于频繁")
    public ApiResponse<LoginResponseVo> trustedDeviceLogin(@Valid @RequestBody TrustedDeviceLoginRequest request,
                                                           HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.loginWithTrustedDevice(
                request.userId(),
                request.deviceFingerprint(),
                request.grantToken(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/refresh")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-refresh", permits = 30, windowSeconds = 60, message = "刷新过于频繁")
    public ApiResponse<LoginResponseVo> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.refreshSession(
                request.refreshToken(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/recovery/send-code")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-recovery-send", permits = 10, windowSeconds = 60, message = "发送过于频繁")
    public ApiResponse<CodeDispatchVo> sendRecoveryCode(@Valid @RequestBody RecoverySendCodeRequest request,
                                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.sendRecoveryCode(
                request.email(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/recovery/verify-code")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-recovery-verify", permits = 20, windowSeconds = 60, message = "验证过于频繁")
    public ApiResponse<RecoveryVerifyVo> verifyRecoveryCode(@Valid @RequestBody RecoveryVerifyCodeRequest request,
                                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.verifyRecoveryCode(
                request.email(),
                request.code(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @PostMapping("/recovery/reset-password")
    @RateLimit(keyType = RateLimit.KeyType.IP, name = "auth-recovery-reset", permits = 10, windowSeconds = 60, message = "重置过于频繁")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody RecoveryResetPasswordRequest request,
                                           HttpServletRequest servletRequest) {
        authService.resetPasswordByRecovery(
                request.recoveryToken(),
                request.newPassword(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        );
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ApiResponse.success();
    }

    @PostMapping("/change-password")
    @RequireLogin
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(LoginUserContext.requireUserId(), request.oldPassword(), request.newPassword());
        return ApiResponse.success();
    }

    @RequireLogin
    @PostMapping("/email/send-bind-code")
    public ApiResponse<CodeDispatchVo> sendBindCode(@Valid @RequestBody EmailBindCodeRequest request,
                                                    HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.sendEmailBindCode(
                LoginUserContext.requireUserId(),
                request.email(),
                request.currentPassword(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @RequireLogin
    @PostMapping("/email/bind")
    public ApiResponse<UserProfileVo> bindEmail(@Valid @RequestBody EmailBindRequest request,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.bindEmail(
                LoginUserContext.requireUserId(),
                request.email(),
                request.code(),
                request.currentPassword(),
                resolveIp(servletRequest),
                resolveUserAgent(servletRequest)
        ));
    }

    @RequireLogin
    @GetMapping("/trusted-devices")
    public ApiResponse<List<TrustedDeviceItemVo>> trustedDevices() {
        return ApiResponse.success(authService.listTrustedDevices(LoginUserContext.requireUserId()));
    }

    @RequireLogin
    @DeleteMapping("/trusted-devices/{deviceId}")
    public ApiResponse<Void> revokeTrustedDevice(@PathVariable Long deviceId) {
        authService.revokeTrustedDevice(LoginUserContext.requireUserId(), deviceId);
        return ApiResponse.success();
    }

    @RequireLogin
    @PostMapping("/trusted-devices/revoke-all")
    public ApiResponse<Void> revokeAllTrustedDevices() {
        authService.revokeAllTrustedDevices(LoginUserContext.requireUserId());
        return ApiResponse.success();
    }

    @RequireLogin
    @GetMapping("/security-events")
    public ApiResponse<List<SecurityEventItemVo>> securityEvents() {
        return ApiResponse.success(authService.listSecurityEvents(LoginUserContext.requireUserId()));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "旧密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空") String newPassword
    ) {
    }

    public record LoginChallengeVerifyRequest(
            @NotBlank(message = "challengeTicket 不能为空") String challengeTicket,
            @NotBlank(message = "验证码不能为空") String code
    ) {
    }

    public record LoginChallengeResendRequest(
            @NotBlank(message = "challengeTicket 不能为空") String challengeTicket
    ) {
    }

    public record TrustedDeviceLoginRequest(
            Long userId,
            @NotBlank(message = "设备指纹不能为空") String deviceFingerprint,
            @NotBlank(message = "授权令牌不能为空") String grantToken
    ) {
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "refreshToken 不能为空") String refreshToken
    ) {
    }

    public record LogoutRequest(
            String refreshToken
    ) {
    }

    public record RecoverySendCodeRequest(
            @NotBlank(message = "邮箱不能为空") String email
    ) {
    }

    public record RecoveryVerifyCodeRequest(
            @NotBlank(message = "邮箱不能为空") String email,
            @NotBlank(message = "验证码不能为空") String code
    ) {
    }

    public record RecoveryResetPasswordRequest(
            @NotBlank(message = "恢复凭证不能为空") String recoveryToken,
            @NotBlank(message = "新密码不能为空") String newPassword
    ) {
    }

    public record EmailBindCodeRequest(
            @NotBlank(message = "邮箱不能为空") String email,
            @NotBlank(message = "当前密码不能为空") String currentPassword
    ) {
    }

    public record EmailBindRequest(
            @NotBlank(message = "邮箱不能为空") String email,
            @NotBlank(message = "验证码不能为空") String code,
            @NotBlank(message = "当前密码不能为空") String currentPassword
    ) {
    }
}
