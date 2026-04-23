package com.echoim.server.service.impl;

import com.echoim.server.common.PageResponse;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.user.UserProfileService;
import com.echoim.server.vo.user.UserPublicProfileVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.echoim.server.vo.user.UserSearchItemVo;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
