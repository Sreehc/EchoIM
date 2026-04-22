package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserProfileVo;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final ImUserMapper imUserMapper;

    public UserProfileServiceImpl(ImUserMapper imUserMapper) {
        this.imUserMapper = imUserMapper;
    }

    @Override
    public UserProfileVo getCurrentProfile(Long userId) {
        UserProfileVo profile = imUserMapper.selectProfileByUserId(userId);
        if (profile == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return profile;
    }

    @Override
    public UserProfileVo updateCurrentProfile(Long userId, UpdateProfileRequestDto requestDto) {
        ImUserEntity entity = imUserMapper.selectById(userId);
        if (entity == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        entity.setNickname(requestDto.getNickname());
        entity.setAvatarUrl(requestDto.getAvatarUrl());
        entity.setGender(requestDto.getGender());
        entity.setSignature(requestDto.getSignature());
        imUserMapper.updateById(entity);
        return getCurrentProfile(userId);
    }
}
