package com.echoim.server.service.block;

import com.echoim.server.vo.block.BlockedUserItemVo;

import java.util.List;

public interface BlockService {

    void blockUser(Long currentUserId, Long targetUserId);

    void unblockUser(Long currentUserId, Long targetUserId);

    List<BlockedUserItemVo> listBlockedUsers(Long currentUserId);

    boolean isBlocked(Long userId, Long targetUserId);

    List<Long> getBlockedUserIds(Long userId);
}
