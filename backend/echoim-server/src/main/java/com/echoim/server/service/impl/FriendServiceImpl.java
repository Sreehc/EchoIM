package com.echoim.server.service.impl;

import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImFriendEntity;
import com.echoim.server.entity.ImFriendRequestEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.entity.ImUserEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImFriendMapper;
import com.echoim.server.mapper.ImFriendRequestMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImUserMapper;
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
    private static final int FRIEND_STATUS_BLOCKED = 2;
    private static final int FRIEND_STATUS_DELETED = 3;
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
    private final ImUserMapper imUserMapper;
    private final ImWsPushService imWsPushService;

    public FriendServiceImpl(ImFriendMapper imFriendMapper,
                             ImFriendRequestMapper imFriendRequestMapper,
                             ImConversationMapper imConversationMapper,
                             ImConversationUserMapper imConversationUserMapper,
                             ImMessageMapper imMessageMapper,
                             ImUserMapper imUserMapper,
                             ImWsPushService imWsPushService) {
        this.imFriendMapper = imFriendMapper;
        this.imFriendRequestMapper = imFriendRequestMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imUserMapper = imUserMapper;
        this.imWsPushService = imWsPushService;
    }

    @Override
    public List<FriendItemVo> listFriends(Long userId) {
        return imFriendMapper.selectFriendListByUserId(userId);
    }

    @Override
    public List<FriendItemVo> listBlockedFriends(Long userId) {
        return imFriendMapper.selectBlockedListByUserId(userId);
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
        pushConversationCreated(request.getFromUserId(), conversation.getId(), message);
        pushConversationCreated(request.getToUserId(), conversation.getId(), message);
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

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long friendUserId) {
        requireTargetUser(friendUserId);
        ImFriendEntity currentRelation = requireExistingRelation(currentUserId, friendUserId);
        ImFriendEntity reverseRelation = imFriendMapper.selectRelationByUserIdAndFriendUserId(friendUserId, currentUserId);
        currentRelation.setStatus(FRIEND_STATUS_DELETED);
        imFriendMapper.updateById(currentRelation);
        if (reverseRelation != null && !Integer.valueOf(FRIEND_STATUS_DELETED).equals(reverseRelation.getStatus())) {
            reverseRelation.setStatus(FRIEND_STATUS_DELETED);
            imFriendMapper.updateById(reverseRelation);
        }
    }

    @Override
    @Transactional
    public void updateRemark(Long currentUserId, Long friendUserId, String remark) {
        requireTargetUser(friendUserId);
        ImFriendEntity relation = requireRelationWithStatus(currentUserId, friendUserId, FRIEND_STATUS_NORMAL);
        relation.setRemark(normalizeRemark(remark));
        imFriendMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void blockFriend(Long currentUserId, Long friendUserId) {
        requireTargetUser(friendUserId);
        ImFriendEntity relation = requireExistingRelation(currentUserId, friendUserId);
        if (Integer.valueOf(FRIEND_STATUS_DELETED).equals(relation.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "好友关系不存在");
        }
        relation.setStatus(FRIEND_STATUS_BLOCKED);
        imFriendMapper.updateById(relation);
    }

    @Override
    @Transactional
    public void unblockFriend(Long currentUserId, Long friendUserId) {
        requireTargetUser(friendUserId);
        ImFriendEntity relation = requireRelationWithStatus(currentUserId, friendUserId, FRIEND_STATUS_BLOCKED);
        relation.setStatus(FRIEND_STATUS_NORMAL);
        imFriendMapper.updateById(relation);
    }

    @Override
    public void validateSingleChatAllowed(Long fromUserId, Long toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单聊目标错误");
        }
        ImUserEntity targetUser = requireTargetUser(toUserId);
        if (!Integer.valueOf(1).equals(targetUser.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "目标用户不可发起私聊");
        }
        ImFriendEntity senderRelation = imFriendMapper.selectRelationByUserIdAndFriendUserId(fromUserId, toUserId);
        ImFriendEntity receiverRelation = imFriendMapper.selectRelationByUserIdAndFriendUserId(toUserId, fromUserId);
        if (senderRelation != null && Integer.valueOf(FRIEND_STATUS_BLOCKED).equals(senderRelation.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "你已拉黑对方");
        }
        if (receiverRelation != null && Integer.valueOf(FRIEND_STATUS_BLOCKED).equals(receiverRelation.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "对方已拉黑你");
        }
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
        ImFriendEntity existing = imFriendMapper.selectRelationByUserIdAndFriendUserId(userId, friendUserId);
        if (existing != null) {
            existing.setStatus(FRIEND_STATUS_NORMAL);
            imFriendMapper.updateById(existing);
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
            existing.setDeleted(0);
            existing.setIsArchived(0);
            existing.setManualUnread(0);
            imConversationUserMapper.updateById(existing);
            return;
        }
        ImConversationUserEntity entity = new ImConversationUserEntity();
        entity.setConversationId(conversationId);
        entity.setUserId(userId);
        entity.setUnreadCount(0);
        entity.setLastReadSeq(0L);
        entity.setIsTop(0);
        entity.setIsMute(0);
        entity.setIsArchived(0);
        entity.setManualUnread(0);
        entity.setDeleted(0);
        imConversationUserMapper.insert(entity);
    }

    private long nextSeqNo(Long conversationId) {
        Long current = imMessageMapper.selectMaxSeqNoByConversationId(conversationId);
        return current == null ? 1L : current + 1;
    }

    private void pushConversationCreated(Long userId, Long conversationId, ImMessageEntity message) {
        var conversation = imConversationMapper.selectConversationItemByUserId(conversationId, userId);
        if (conversation == null) {
            return;
        }
        imWsPushService.pushConversationChange(userId, "CONVERSATION_CREATED", conversation, toWsMessageItem(message));
    }

    private WsMessageItem toWsMessageItem(ImMessageEntity entity) {
        WsMessageItem item = new WsMessageItem();
        item.setMessageId(entity.getId());
        item.setConversationId(entity.getConversationId());
        item.setSeqNo(entity.getSeqNo());
        item.setClientMsgId(entity.getClientMsgId());
        item.setFromUserId(entity.getFromUserId());
        item.setToUserId(entity.getToUserId());
        item.setMsgType("SYSTEM");
        item.setContent(entity.getContent());
        item.setFileId(entity.getFileId());
        item.setSendStatus(entity.getSendStatus());
        item.setSentAt(entity.getSentAt());
        return item;
    }

    private ImUserEntity requireTargetUser(Long friendUserId) {
        ImUserEntity entity = imUserMapper.selectById(friendUserId);
        if (entity == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return entity;
    }

    private ImFriendEntity requireExistingRelation(Long currentUserId, Long friendUserId) {
        ImFriendEntity relation = imFriendMapper.selectRelationByUserIdAndFriendUserId(currentUserId, friendUserId);
        if (relation == null) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "好友关系不存在");
        }
        return relation;
    }

    private ImFriendEntity requireRelationWithStatus(Long currentUserId, Long friendUserId, int status) {
        ImFriendEntity relation = requireExistingRelation(currentUserId, friendUserId);
        if (!Integer.valueOf(status).equals(relation.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "好友关系状态不正确");
        }
        return relation;
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String normalized = remark.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
