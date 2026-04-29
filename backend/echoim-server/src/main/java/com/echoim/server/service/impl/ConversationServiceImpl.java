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
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.service.message.MessageViewService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final String SAVED_BIZ_KEY_PREFIX = "saved_";

    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImSingleChatService imSingleChatService;
    private final FileService fileService;
    private final MessageViewService messageViewService;
    private final FriendService friendService;

    public ConversationServiceImpl(ImConversationMapper imConversationMapper,
                                   ImConversationUserMapper imConversationUserMapper,
                                   ImMessageMapper imMessageMapper,
                                   ImSingleChatService imSingleChatService,
                                   FileService fileService,
                                   MessageViewService messageViewService,
                                   FriendService friendService) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imSingleChatService = imSingleChatService;
        this.fileService = fileService;
        this.messageViewService = messageViewService;
        this.friendService = friendService;
    }

    @Override
    public PageResponse<ConversationItemVo> pageCurrentUserConversations(Long userId, ConversationPageQueryDto queryDto) {
        long pageNo = normalizePageNo(queryDto.getPageNo());
        long pageSize = normalizePageSize(queryDto.getPageSize());
        long offset = (pageNo - 1) * pageSize;
        String folder = normalizeFolder(queryDto.getFolder(), queryDto.getArchived());
        List<ConversationItemVo> allConversations = imConversationMapper.selectAllConversationsByUserId(userId)
                .stream()
                .peek(this::applyFolderHints)
                .filter(item -> matchesFolder(item, folder))
                .toList();
        int fromIndex = (int) Math.min(offset, allConversations.size());
        int toIndex = (int) Math.min(offset + pageSize, allConversations.size());
        return new PageResponse<>(allConversations.subList(fromIndex, toIndex), pageNo, pageSize, allConversations.size());
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
    public ConversationItemVo createSingleConversation(Long userId, Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "目标用户不能为空");
        }
        if (userId.equals(targetUserId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能和自己创建私聊");
        }

        friendService.validateSingleChatAllowed(userId, targetUserId);
        ImConversationEntity conversation = ensureSingleConversation(userId, targetUserId);
        ensureConversationUser(conversation.getId(), userId, true);
        ensureConversationUser(conversation.getId(), targetUserId, false);
        ConversationItemVo item = imConversationMapper.selectConversationItemByUserId(conversation.getId(), userId);
        if (item == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话创建失败");
        }
        applyFolderHints(item);
        return item;
    }

    @Override
    public ConversationItemVo createSavedConversation(Long userId) {
        ImConversationEntity conversation = ensureSavedConversation(userId);
        ensureConversationUser(conversation.getId(), userId, true);
        ConversationItemVo item = imConversationMapper.selectConversationItemByUserId(conversation.getId(), userId);
        if (item == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "Saved Messages 创建失败");
        }
        applyFolderHints(item);
        return item;
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
    public void updateArchive(Long userId, Long conversationId, boolean archived) {
        requireConversationUser(userId, conversationId);
        imConversationUserMapper.updateArchive(conversationId, userId, archived ? 1 : 0);
    }

    @Override
    public void markConversationUnread(Long userId, Long conversationId, boolean unread) {
        if (!unread) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持标记为未读");
        }
        requireConversationUser(userId, conversationId);
        imConversationUserMapper.updateManualUnread(conversationId, userId, 1);
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

    private ImConversationEntity ensureSingleConversation(Long fromUserId, Long toUserId) {
        long min = Math.min(fromUserId, toUserId);
        long max = Math.max(fromUserId, toUserId);
        String bizKey = min + "_" + max;
        ImConversationEntity entity = imConversationMapper.selectSingleConversationByBizKey(bizKey);
        if (entity != null) {
            if (!Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(entity.getStatus())) {
                entity.setStatus(CONVERSATION_STATUS_NORMAL);
                imConversationMapper.updateById(entity);
            }
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

    private ImConversationEntity ensureSavedConversation(Long userId) {
        String bizKey = SAVED_BIZ_KEY_PREFIX + userId;
        ImConversationEntity entity = imConversationMapper.selectSingleConversationByBizKey(bizKey);
        if (entity != null) {
            if (!Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(entity.getStatus())) {
                entity.setStatus(CONVERSATION_STATUS_NORMAL);
                imConversationMapper.updateById(entity);
            }
            return entity;
        }

        ImConversationEntity conversation = new ImConversationEntity();
        conversation.setConversationType(CONVERSATION_TYPE_SINGLE);
        conversation.setBizKey(bizKey);
        conversation.setConversationName("Saved Messages");
        conversation.setStatus(CONVERSATION_STATUS_NORMAL);
        imConversationMapper.insert(conversation);
        return conversation;
    }

    private void ensureConversationUser(Long conversationId, Long userId, boolean activateInbox) {
        ImConversationUserEntity existing = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (existing != null) {
            existing.setDeleted(0);
            existing.setManualUnread(0);
            if (activateInbox) {
                existing.setIsArchived(0);
            }
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

    private void validateSwitchValue(Integer value, String message) {
        if (value == null || (value != 0 && value != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private int normalizeArchived(Integer archived) {
        if (archived == null) {
            return 0;
        }
        if (archived != 0 && archived != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "archived 参数错误");
        }
        return archived;
    }

    private String normalizeFolder(String folder, Integer archived) {
        if (folder == null || folder.isBlank()) {
            return normalizeArchived(archived) == 1 ? "archived" : "inbox";
        }
        String normalizedFolder = folder.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedFolder) {
            case "inbox", "archived", "unread", "single", "group", "channel" -> normalizedFolder;
            default -> throw new BizException(ErrorCode.PARAM_ERROR, "folder 参数错误");
        };
    }

    private boolean matchesFolder(ConversationItemVo item, String folder) {
        boolean archived = Boolean.TRUE.equals(item.getArchived());
        boolean manualUnread = Boolean.TRUE.equals(item.getManualUnread());
        int unreadCount = item.getUnreadCount() == null ? 0 : item.getUnreadCount();
        int conversationType = item.getConversationType() == null ? 0 : item.getConversationType();
        return switch (folder) {
            case "archived" -> archived;
            case "unread" -> !archived && (unreadCount > 0 || manualUnread);
            case "single" -> !archived && conversationType == CONVERSATION_TYPE_SINGLE;
            case "group" -> !archived && conversationType == CONVERSATION_TYPE_GROUP;
            case "channel" -> !archived && conversationType == CONVERSATION_TYPE_CHANNEL;
            case "inbox" -> !archived;
            default -> true;
        };
    }

    private void applyFolderHints(ConversationItemVo item) {
        List<String> folderHints = new ArrayList<>();
        boolean archived = Boolean.TRUE.equals(item.getArchived());
        if (archived) {
            folderHints.add("archived");
        } else {
            folderHints.add("inbox");
        }
        if ((item.getUnreadCount() != null && item.getUnreadCount() > 0) || Boolean.TRUE.equals(item.getManualUnread())) {
            folderHints.add("unread");
        }
        if (Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(item.getConversationType())) {
            folderHints.add("single");
        } else if (Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(item.getConversationType())) {
            folderHints.add("group");
        } else if (Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(item.getConversationType())) {
            folderHints.add("channel");
        }
        item.setFolderHints(folderHints);
    }

    private long normalizePageNo(Long pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 20L : pageSize;
    }
}
