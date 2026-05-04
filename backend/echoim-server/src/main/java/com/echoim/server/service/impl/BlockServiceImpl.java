package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImBlockUserEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImBlockUserMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.block.BlockService;
import com.echoim.server.vo.block.BlockedUserItemVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlockServiceImpl implements BlockService {

    private final ImBlockUserMapper imBlockUserMapper;
    private final ImUserMapper imUserMapper;

    public BlockServiceImpl(ImBlockUserMapper imBlockUserMapper, ImUserMapper imUserMapper) {
        this.imBlockUserMapper = imBlockUserMapper;
        this.imUserMapper = imUserMapper;
    }

    @Override
    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能屏蔽自己");
        }
        ImUserEntity target = imUserMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        ImBlockUserEntity existing = imBlockUserMapper.selectByUserAndBlocked(currentUserId, targetUserId);
        if (existing != null) {
            throw new BizException(ErrorCode.ALREADY_BLOCKED, "已屏蔽该用户");
        }
        ImBlockUserEntity entity = new ImBlockUserEntity();
        entity.setUserId(currentUserId);
        entity.setBlockedUserId(targetUserId);
        entity.setCreatedAt(LocalDateTime.now());
        imBlockUserMapper.insert(entity);
    }

    @Override
    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        ImBlockUserEntity existing = imBlockUserMapper.selectByUserAndBlocked(currentUserId, targetUserId);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_BLOCKED, "未屏蔽该用户");
        }
        imBlockUserMapper.deleteById(existing.getId());
    }

    @Override
    public List<BlockedUserItemVo> listBlockedUsers(Long currentUserId) {
        return imBlockUserMapper.selectBlockedItemsByUserId(currentUserId);
    }

    @Override
    public boolean isBlocked(Long userId, Long targetUserId) {
        return imBlockUserMapper.selectByUserAndBlocked(userId, targetUserId) != null;
    }

    @Override
    public List<Long> getBlockedUserIds(Long userId) {
        return imBlockUserMapper.selectBlockedUserIdsByUserId(userId);
    }
}
