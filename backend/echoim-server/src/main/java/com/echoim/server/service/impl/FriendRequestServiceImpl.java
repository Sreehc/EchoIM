package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.friend.CreateFriendRequestDto;
import com.echoim.server.entity.ImFriendRequestEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.mapper.ImFriendRequestMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.friend.FriendRequestService;
import com.echoim.server.vo.friend.FriendRequestCreateVo;
import org.springframework.stereotype.Service;

@Service
public class FriendRequestServiceImpl implements FriendRequestService {

    private static final int USER_STATUS_NORMAL = 1;
    private static final int FRIEND_REQUEST_PENDING = 0;

    private final ImFriendRequestMapper imFriendRequestMapper;
    private final ImUserMapper imUserMapper;

    public FriendRequestServiceImpl(ImFriendRequestMapper imFriendRequestMapper, ImUserMapper imUserMapper) {
        this.imFriendRequestMapper = imFriendRequestMapper;
        this.imUserMapper = imUserMapper;
    }

    @Override
    public FriendRequestCreateVo create(Long fromUserId, CreateFriendRequestDto requestDto) {
        Long toUserId = requestDto.getToUserId();
        if (fromUserId.equals(toUserId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能给自己发起好友申请");
        }

        ImUserEntity targetUser = imUserMapper.selectById(toUserId);
        if (targetUser == null || targetUser.getStatus() == null || targetUser.getStatus() != USER_STATUS_NORMAL) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "目标用户不存在");
        }

        if (imFriendRequestMapper.countExistingFriendRelation(fromUserId, toUserId) > 0) {
            throw new BizException(ErrorCode.ALREADY_FRIEND, "已是好友");
        }

        if (imFriendRequestMapper.countPendingRequest(fromUserId, toUserId) > 0) {
            throw new BizException(ErrorCode.FRIEND_REQUEST_DUPLICATE, "好友申请重复提交");
        }

        ImFriendRequestEntity entity = new ImFriendRequestEntity();
        entity.setFromUserId(fromUserId);
        entity.setToUserId(toUserId);
        entity.setApplyMsg(requestDto.getApplyMsg());
        entity.setStatus(FRIEND_REQUEST_PENDING);
        imFriendRequestMapper.insert(entity);

        FriendRequestCreateVo responseVo = new FriendRequestCreateVo();
        responseVo.setRequestId(entity.getId());
        responseVo.setToUserId(toUserId);
        responseVo.setStatus(FRIEND_REQUEST_PENDING);
        return responseVo;
    }
}
