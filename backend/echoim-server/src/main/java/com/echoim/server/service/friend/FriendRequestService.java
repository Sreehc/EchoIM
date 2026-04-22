package com.echoim.server.service.friend;

import com.echoim.server.dto.friend.CreateFriendRequestDto;
import com.echoim.server.vo.friend.FriendRequestCreateVo;

public interface FriendRequestService {

    FriendRequestCreateVo create(Long fromUserId, CreateFriendRequestDto requestDto);
}
