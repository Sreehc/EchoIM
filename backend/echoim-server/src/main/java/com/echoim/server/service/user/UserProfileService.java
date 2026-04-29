package com.echoim.server.service.user;

import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.PageResponse;
import com.echoim.server.vo.user.UserPublicProfileVo;
import com.echoim.server.vo.user.UserProfileVo;
import com.echoim.server.vo.user.UserSearchItemVo;

import java.util.Map;

public interface UserProfileService {

    UserProfileVo getCurrentProfile(Long userId);

    UserProfileVo updateCurrentProfile(Long userId, UpdateProfileRequestDto requestDto);

    PageResponse<UserSearchItemVo> searchUsers(Long currentUserId, String keyword, long pageNo, long pageSize);

    UserPublicProfileVo getPublicProfile(Long currentUserId, Long targetUserId);

    UserPublicProfileVo getPublicProfileByUsername(Long currentUserId, String username);

    Map<String, Object> checkUsername(Long currentUserId, String username);
}
