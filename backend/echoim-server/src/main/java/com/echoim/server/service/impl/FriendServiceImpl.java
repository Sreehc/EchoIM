package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImFriendEntity;
import com.echoim.server.entity.ImFriendRequestEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImFriendMapper;
import com.echoim.server.mapper.ImFriendRequestMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.vo.friend.FriendItemVo;
import com.echoim.server.vo.friend.FriendRequestItemVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {

    private static final int FRIEND_STATUS_NORMAL = 1;
    private static final int REQUEST_PENDING = 0;
    private static final int REQUEST_APPROVED = 1;
    private static final int REQUEST_REJECTED = 2;
    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int MESSAGE_TYPE_SYSTEM = 6;
    private static final int MESSAGE_STATUS_SENT = 1;

    private final ImFriendMapper imFriendMapper;
    private final ImFriendRequestMapper imFriendRequestMapper;
    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;

    public FriendServiceImpl(ImFriendMapper imFriendMapper,
                             ImFriendRequestMapper imFriendRequestMapper,
                             ImConversationMapper imConversationMapper,
                             ImConversationUserMapper imConversationUserMapper,
                             ImMessageMapper imMessageMapper) {
        this.imFriendMapper = imFriendMapper;
        this.imFriendRequestMapper = imFriendRequestMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
    }

    @Override
    public List<FriendItemVo> listFriends(Long userId) {
        return imFriendMapper.selectFriendListByUserId(userId);
    }

    @Override
    public List<FriendRequestItemVo> listRelatedRequests(Long userId) {
        return imFriendRequestMapper.selectRelatedRequests(userId);
    }

    @Override
    @Transactional
    public void approveRequest(Long currentUserId, Long requestId) {
        ImFriendRequestEntity request = validateRequestForAction(currentUserId, requestId);
        if (imFriendRequestMapper.countExistingFriendRelation(request.getFromUserId(), request.getToUserId()) > 0) {
            throw new BizException(ErrorCode.ALREADY_FRIEND, "已是好友");
        }

        request.setStatus(REQUEST_APPROVED);
        request.setHandledBy(currentUserId);
        request.setHandledAt(LocalDateTime.now());
        imFriendRequestMapper.updateById(request);

        insertFriendIfAbsent(request.getFromUserId(), request.getToUserId());
        insertFriendIfAbsent(request.getToUserId(), request.getFromUserId());

        ImConversationEntity conversation = ensureSingleConversation(request.getFromUserId(), request.getToUserId());
        ensureConversationUser(conversation.getId(), request.getFromUserId());
        ensureConversationUser(conversation.getId(), request.getToUserId());

        ImMessageEntity message = new ImMessageEntity();
        message.setConversationId(conversation.getId());
        message.setConversationType(CONVERSATION_TYPE_SINGLE);
        message.setSeqNo(nextSeqNo(conversation.getId()));
        message.setClientMsgId("approve-" + request.getId());
        message.setFromUserId(request.getFromUserId());
        message.setToUserId(request.getToUserId());
        message.setMsgType(MESSAGE_TYPE_SYSTEM);
        message.setContent(request.getApplyMsg());
        message.setSendStatus(MESSAGE_STATUS_SENT);
        message.setSentAt(LocalDateTime.now());
        imMessageMapper.insert(message);

        imConversationMapper.updateLastMessage(conversation.getId(), message.getId(), request.getApplyMsg());
    }

    @Override
    @Transactional
    public void rejectRequest(Long currentUserId, Long requestId) {
        ImFriendRequestEntity request = validateRequestForAction(currentUserId, requestId);
        request.setStatus(REQUEST_REJECTED);
        request.setHandledBy(currentUserId);
        request.setHandledAt(LocalDateTime.now());
        imFriendRequestMapper.updateById(request);
    }

    private ImFriendRequestEntity validateRequestForAction(Long currentUserId, Long requestId) {
        ImFriendRequestEntity request = imFriendRequestMapper.selectById(requestId);
        if (request == null || !currentUserId.equals(request.getToUserId())) {
            throw new BizException(ErrorCode.FRIEND_REQUEST_NOT_FOUND, "好友申请不存在");
        }
        if (request.getStatus() == null || request.getStatus() != REQUEST_PENDING) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "好友申请已处理");
        }
        return request;
    }

    private void insertFriendIfAbsent(Long userId, Long friendUserId) {
        Long count = imFriendMapper.selectCount(new LambdaQueryWrapper<ImFriendEntity>()
                .eq(ImFriendEntity::getUserId, userId)
                .eq(ImFriendEntity::getFriendUserId, friendUserId)
                .eq(ImFriendEntity::getStatus, FRIEND_STATUS_NORMAL));
        if (count != null && count > 0) {
            return;
        }
        ImFriendEntity entity = new ImFriendEntity();
        entity.setUserId(userId);
        entity.setFriendUserId(friendUserId);
        entity.setStatus(FRIEND_STATUS_NORMAL);
        imFriendMapper.insert(entity);
    }

    private ImConversationEntity ensureSingleConversation(Long fromUserId, Long toUserId) {
        long min = Math.min(fromUserId, toUserId);
        long max = Math.max(fromUserId, toUserId);
        String bizKey = min + "_" + max;
        ImConversationEntity entity = imConversationMapper.selectSingleConversationByBizKey(bizKey);
        if (entity != null) {
            return entity;
        }

        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setConversationType(CONVERSATION_TYPE_SINGLE);
        conversation.setBizKey(bizKey);
        conversation.setConversationName(bizKey);
        conversation.setStatus(CONVERSATION_STATUS_NORMAL);
        imConversationMapper.insert(conversation);
        return conversation;
    }

    private void ensureConversationUser(Long conversationId, Long userId) {
        ImConversationUserEntity existing = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (existing != null) {
            return;
        }
        ImConversationUserEntity entity = new ImConversationUserEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(userId);
        entity.setUnreadCount(0);
        entity.setLastReadSeq(0L);
        entity.setIsTop(0);
        entity.setIsMute(0);
        entity.setDeleted(0);
        imConversationUserMapper.insert(entity);
    }

    private long nextSeqNo(Long conversationId) {
        Long current = imMessageMapper.selectMaxSeqNoByConversationId(conversationId);
        return current == null ? 1L : current + 1;
    }
}
