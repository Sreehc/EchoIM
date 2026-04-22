package com.echoim.server.service.user;

import com.echoim.server.dto.user.UpdateProfileRequestDto;
import com.echoim.server.vo.user.UserProfileVo;

public interface UserProfileService {

    UserProfileVo getCurrentProfile(Long userId);

    UserProfileVo updateCurrentProfile(Long userId, UpdateProfileRequestDto requestDto);
}
