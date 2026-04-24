package com.echoim.server.service.friend;

import com.echoim.server.vo.friend.FriendItemVo;
import com.echoim.server.vo.friend.FriendRequestItemVo;

import java.util.List;

public interface FriendService {

    List<FriendItemVo> listFriends(Long userId);

    List<FriendItemVo> listBlockedFriends(Long userId);

    List<FriendRequestItemVo> listRelatedRequests(Long userId);

    void approveRequest(Long currentUserId, Long requestId);

    void rejectRequest(Long currentUserId, Long requestId);

    void deleteFriend(Long currentUserId, Long friendUserId);

    void updateRemark(Long currentUserId, Long friendUserId, String remark);

    void blockFriend(Long currentUserId, Long friendUserId);

    void unblockFriend(Long currentUserId, Long friendUserId);

    void validateSingleChatAllowed(Long fromUserId, Long toUserId);
}
