package com.echoim.server.service.auth;

import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.RegisterResponseVo;

public interface AuthService {

    RegisterResponseVo register(RegisterRequestDto requestDto);

    LoginResponseVo login(LoginRequestDto requestDto);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
