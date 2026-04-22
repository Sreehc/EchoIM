package com.echoim.server.service.impl;

import com.echoim.server.common.auth.LoginUser;
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

    public AuthServiceImpl(ImUserMapper imUserMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.imUserMapper = imUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
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
        return responseVo;
    }

    @Override
    public LoginResponseVo login(LoginRequestDto requestDto) {
        ImUserEntity userEntity = imUserMapper.selectByUsername(requestDto.getUsername());
        if (userEntity == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!isUserStatusNormal(userEntity.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户状态不可用");
        }
        if (!passwordEncoder.matches(requestDto.getPassword(), userEntity.getPasswordHash())) {
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
        return responseVo;
    }

    private boolean isUserStatusNormal(Integer status) {
        return status != null && status == USER_STATUS_NORMAL;
    }
}
