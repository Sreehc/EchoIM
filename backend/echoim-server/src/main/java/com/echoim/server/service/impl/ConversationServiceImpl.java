package com.echoim.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.PageResponse;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.service.conversation.ConversationService;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.message.MessageViewService;
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
    private final FileService fileService;
    private final MessageViewService messageViewService;

    public ConversationServiceImpl(ImConversationMapper imConversationMapper,
                                   ImConversationUserMapper imConversationUserMapper,
                                   ImMessageMapper imMessageMapper,
                                   ImSingleChatService imSingleChatService,
                                   FileService fileService,
                                   MessageViewService messageViewService) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imSingleChatService = imSingleChatService;
        this.fileService = fileService;
        this.messageViewService = messageViewService;
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
        fileService.enrichMessages(userId, list);
        messageViewService.enrichMessages(userId, list);
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    @Override
    public void readConversation(Long userId, Long conversationId, Long lastReadSeq) {
        imSingleChatService.read(userId, conversationId, lastReadSeq, null, null);
    }

    @Override
    public void updateTop(Long userId, Long conversationId, Integer isTop) {
        validateSwitchValue(isTop, "置顶状态错误");
        requireConversationUser(userId, conversationId);
        imConversationUserMapper.updateTop(conversationId, userId, isTop);
    }

    @Override
    public void updateMute(Long userId, Long conversationId, Integer isMute) {
        validateSwitchValue(isMute, "免打扰状态错误");
        requireConversationUser(userId, conversationId);
        imConversationUserMapper.updateMute(conversationId, userId, isMute);
    }

    @Override
    public void deleteConversation(Long userId, Long conversationId) {
        requireConversationUser(userId, conversationId);
        imConversationUserMapper.hideConversation(conversationId, userId);
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

    private void requireConversationUser(Long userId, Long conversationId) {
        ImConversationEntity conversation = imConversationMapper.selectById(conversationId);
        ImConversationUserEntity conversationUser = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (conversation == null || conversationUser == null || !Integer.valueOf(1).equals(conversation.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
    }

    private void validateSwitchValue(Integer value, String message) {
        if (value == null || (value != 0 && value != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 20L : pageSize;
    }
}
