package com.echoim.server.service.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.auth.LoginRequestDto;
import com.echoim.server.dto.auth.RegisterRequestDto;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.auth.AuthService;
import com.echoim.server.service.token.TokenService;
import com.echoim.server.vo.auth.LoginResponseVo;
import com.echoim.server.vo.auth.LoginUserVo;
import com.echoim.server.vo.auth.RegisterResponseVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int USER_STATUS_NORMAL = 1;

    private final ImUserMapper imUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(ImUserMapper imUserMapper,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           AuditLogService auditLogService) {
        this.imUserMapper = imUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
    }

    @Override
    public RegisterResponseVo register(RegisterRequestDto requestDto) {
        ImUserEntity existingUser = imUserMapper.selectByUsername(requestDto.getUsername());
        if (existingUser != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        ImUserEntity entity = new ImUserEntity();
        entity.setUserNo("TMP_" + System.nanoTime());
        entity.setUsername(requestDto.getUsername());
        entity.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        entity.setNickname(requestDto.getNickname());
        entity.setStatus(USER_STATUS_NORMAL);
        imUserMapper.insert(entity);

        entity.setUserNo("E" + entity.getId());
        imUserMapper.updateById(entity);

        RegisterResponseVo responseVo = new RegisterResponseVo();
        responseVo.setUserId(entity.getId());
        responseVo.setUsername(entity.getUsername());
        responseVo.setNickname(entity.getNickname());
        auditLogService.log("AUTH_REGISTER", java.util.Map.of("userId", entity.getId(), "username", entity.getUsername()));
        return responseVo;
    }

    @Override
    public LoginResponseVo login(LoginRequestDto requestDto) {
        ImUserEntity userEntity = imUserMapper.selectByUsername(requestDto.getUsername());
        if (userEntity == null) {
            auditLogService.log("AUTH_LOGIN_FAIL", java.util.Map.of("username", requestDto.getUsername(), "reason", "USER_NOT_FOUND"));
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!isUserStatusNormal(userEntity.getStatus())) {
            auditLogService.log("AUTH_LOGIN_FAIL", java.util.Map.of("userId", userEntity.getId(), "username", userEntity.getUsername(), "reason", "STATUS_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户状态不可用");
        }
        if (!passwordEncoder.matches(requestDto.getPassword(), userEntity.getPasswordHash())) {
            auditLogService.log("AUTH_LOGIN_FAIL", java.util.Map.of("userId", userEntity.getId(), "username", userEntity.getUsername(), "reason", "PASSWORD_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        userEntity.setLastLoginAt(LocalDateTime.now());
        imUserMapper.updateById(userEntity);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userEntity.getId());
        loginUser.setUsername(userEntity.getUsername());
        loginUser.setTokenType("user");

        LoginUserVo loginUserVo = new LoginUserVo();
        loginUserVo.setUserId(userEntity.getId());
        loginUserVo.setUsername(userEntity.getUsername());
        loginUserVo.setNickname(userEntity.getNickname());
        loginUserVo.setAvatarUrl(userEntity.getAvatarUrl());

        LoginResponseVo responseVo = new LoginResponseVo();
        responseVo.setToken(tokenService.generateToken(loginUser));
        responseVo.setTokenType("Bearer");
        responseVo.setExpiresIn(tokenService.getExpireSeconds());
        responseVo.setUserInfo(loginUserVo);
        auditLogService.log("AUTH_LOGIN_SUCCESS", java.util.Map.of("userId", userEntity.getId(), "username", userEntity.getUsername()));
        return responseVo;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        ImUserEntity userEntity = imUserMapper.selectById(userId);
        if (userEntity == null) {
            auditLogService.log("AUTH_PASSWORD_CHANGE_FAIL", java.util.Map.of("userId", userId, "reason", "USER_NOT_FOUND"));
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, userEntity.getPasswordHash())) {
            auditLogService.log("AUTH_PASSWORD_CHANGE_FAIL", java.util.Map.of("userId", userId, "reason", "OLD_PASSWORD_INVALID"));
            throw new BizException(ErrorCode.UNAUTHORIZED, "旧密码不正确");
        }

        String normalizedNewPassword = newPassword == null ? "" : newPassword.trim();
        if (normalizedNewPassword.length() < 6) {
            auditLogService.log("AUTH_PASSWORD_CHANGE_FAIL", java.util.Map.of("userId", userId, "reason", "PASSWORD_TOO_SHORT"));
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码至少需要 6 位");
        }
        if (passwordEncoder.matches(normalizedNewPassword, userEntity.getPasswordHash())) {
            auditLogService.log("AUTH_PASSWORD_CHANGE_FAIL", java.util.Map.of("userId", userId, "reason", "PASSWORD_UNCHANGED"));
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码不能与旧密码相同");
        }

        userEntity.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        imUserMapper.updateById(userEntity);
        auditLogService.log("AUTH_PASSWORD_CHANGE_SUCCESS", java.util.Map.of("userId", userId));
    }

    private boolean isUserStatusNormal(Integer status) {
        return status != null && status == USER_STATUS_NORMAL;
    }
}
