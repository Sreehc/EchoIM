package com.echoim.server.service.auth;

import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.vo.auth.CodeDispatchVo;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.RecoveryVerifyVo;
import com.echoim.server.vo.auth.RegisterResponseVo;
import com.echoim.server.vo.auth.SecurityEventItemVo;
import com.echoim.server.vo.auth.TrustedDeviceItemVo;
import com.echoim.server.vo.user.UserProfileVo;

import java.util.List;

public interface AuthService {

    RegisterResponseVo register(RegisterRequestDto requestDto);

    LoginResponseVo login(LoginRequestDto requestDto, String ip, String userAgent);

    LoginResponseVo verifyLoginChallenge(String challengeTicket, String code, String ip, String userAgent);

    CodeDispatchVo resendLoginChallenge(String challengeTicket, String ip, String userAgent);

    LoginResponseVo loginWithTrustedDevice(Long userId, String deviceFingerprint, String grantToken, String ip, String userAgent);

    void changePassword(Long userId, String oldPassword, String newPassword);

    CodeDispatchVo sendRecoveryCode(String email, String ip, String userAgent);

    RecoveryVerifyVo verifyRecoveryCode(String email, String code, String ip, String userAgent);

    void resetPasswordByRecovery(String recoveryToken, String newPassword, String ip, String userAgent);

    CodeDispatchVo sendEmailBindCode(Long userId, String email, String currentPassword, String ip, String userAgent);

    UserProfileVo bindEmail(Long userId, String email, String code, String currentPassword, String ip, String userAgent);

    List<TrustedDeviceItemVo> listTrustedDevices(Long userId);

    void revokeTrustedDevice(Long userId, Long deviceId);

    void revokeAllTrustedDevices(Long userId);

    List<SecurityEventItemVo> listSecurityEvents(Long userId);
}
