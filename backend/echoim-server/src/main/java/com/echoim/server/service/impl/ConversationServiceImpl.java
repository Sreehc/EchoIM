package com.echoim.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.service.conversation.ConversationService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImSingleChatService imSingleChatService;

    public ConversationServiceImpl(ImConversationMapper imConversationMapper,
                                   ImConversationUserMapper imConversationUserMapper,
                                   ImMessageMapper imMessageMapper,
                                   ImSingleChatService imSingleChatService) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imSingleChatService = imSingleChatService;
    }

    @Override
    public PageResponse<ConversationItemVo> pageCurrentUserConversations(Long userId, ConversationPageQueryDto queryDto) {
        long pageNo = normalizePageNo(queryDto.getPageNo());
        long pageSize = normalizePageSize(queryDto.getPageSize());
        long offset = (pageNo - 1) * pageSize;

        List<ConversationItemVo> list = imConversationMapper.selectConversationPageByUserId(userId, offset, pageSize);
        long total = imConversationMapper.countConversationByUserId(userId);
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    @Override
    public PageResponse<MessageItemVo> pageConversationMessages(Long userId, Long conversationId, MessagePageQueryDto queryDto) {
        long pageNo = normalizePageNo(queryDto.getPageNo());
        long pageSize = normalizePageSize(queryDto.getPageSize());
        long offset = (pageNo - 1) * pageSize;

        if (queryDto.getAfterSeq() != null && queryDto.getMaxSeqNo() != null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "afterSeq 和 maxSeqNo 不能同时传");
        }
        requireActiveConversation(userId, conversationId);

        List<MessageItemVo> list;
        long total;
        if (queryDto.getAfterSeq() != null) {
            pageNo = 1L;
            list = imMessageMapper.selectMessageAfterSeqByConversationIdAndUserId(conversationId, userId, queryDto.getAfterSeq(), pageSize);
            total = imMessageMapper.countMessageAfterSeqByConversationIdAndUserId(conversationId, userId, queryDto.getAfterSeq());
        } else if (queryDto.getMaxSeqNo() != null) {
            pageNo = 1L;
            list = imMessageMapper.selectMessageCursorByConversationIdAndUserId(conversationId, userId, queryDto.getMaxSeqNo(), pageSize);
            Collections.reverse(list);
            total = imMessageMapper.countMessageByConversationIdAndUserId(conversationId, userId);
        } else {
            list = imMessageMapper.selectMessagePageByConversationIdAndUserId(conversationId, userId, offset, pageSize);
            Collections.reverse(list);
            total = imMessageMapper.countMessageByConversationIdAndUserId(conversationId, userId);
        }
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    @Override
    public void readConversation(Long userId, Long conversationId, Long lastReadSeq) {
        imSingleChatService.read(userId, conversationId, lastReadSeq, null, null);
    }

    private void requireActiveConversation(Long userId, Long conversationId) {
        Long ownedCount = imConversationUserMapper.selectCount(new LambdaQueryWrapper<ImConversationUserEntity>()
                .eq(ImConversationUserEntity::getConversationId, conversationId)
                .eq(ImConversationUserEntity::getUserId, userId)
                .eq(ImConversationUserEntity::getDeleted, 0));
        if (ownedCount == null || ownedCount == 0L) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
    }

    private long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 20L : pageSize;
    }
}
