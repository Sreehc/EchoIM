package com.echoim.server.service.impl;

import com.echoim.server.common.PageResponse;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.common.util.UsernameRules;
import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserPublicProfileVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.echoim.server.vo.user.UserSearchItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final long MAX_PAGE_SIZE = 100;

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
        if (requestDto.getUsername() != null) {
            String normalizedUsername = normalizeAndValidateUsername(requestDto.getUsername());
            ImUserEntity existing = imUserMapper.selectByUsernameExcludingUserId(normalizedUsername, userId);
            if (existing != null) {
                throw new BizException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
            }
            entity.setUsername(normalizedUsername);
        }
        entity.setNickname(requestDto.getNickname());
        entity.setAvatarUrl(requestDto.getAvatarUrl());
        entity.setGender(requestDto.getGender());
        entity.setSignature(requestDto.getSignature());
        imUserMapper.updateById(entity);
        return getCurrentProfile(userId);
    }

    @Override
    public PageResponse<UserSearchItemVo> searchUsers(Long currentUserId, String keyword, long pageNo, long pageSize) {
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR, "pageSize 最大为 100");
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        long offset = (pageNo - 1) * pageSize;
        List<UserSearchItemVo> list = imUserMapper.selectSearchPage(currentUserId, normalizedKeyword, offset, pageSize);
        long total = imUserMapper.countSearchUsers(currentUserId, normalizedKeyword);
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    @Override
    public UserPublicProfileVo getPublicProfile(Long currentUserId, Long targetUserId) {
        UserPublicProfileVo profile = imUserMapper.selectPublicProfileByUserId(currentUserId, targetUserId);
        if (profile == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return profile;
    }

    @Override
    public UserPublicProfileVo getPublicProfileByUsername(Long currentUserId, String username) {
        String normalizedUsername = normalizeAndValidateUsername(username);
        UserPublicProfileVo profile = imUserMapper.selectPublicProfileByUsername(currentUserId, normalizedUsername);
        if (profile == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return profile;
    }

    @Override
    public Map<String, Object> checkUsername(Long currentUserId, String username) {
        String normalizedUsername = normalizeAndValidateUsername(username);
        ImUserEntity existing = imUserMapper.selectByUsernameExcludingUserId(normalizedUsername, currentUserId);
        return Map.of(
                "available", existing == null,
                "username", normalizedUsername
        );
    }

    private String normalizeAndValidateUsername(String username) {
        String normalizedUsername = UsernameRules.normalize(username);
        if (!UsernameRules.isValid(normalizedUsername)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名需为 3-24 位字母、数字或下划线，且不能以下划线开头或结尾");
        }
        return normalizedUsername;
    }
}
