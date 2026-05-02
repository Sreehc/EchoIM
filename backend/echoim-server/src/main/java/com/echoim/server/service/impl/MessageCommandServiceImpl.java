package com.echoim.server.service.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.util.ContentSanitizer;
import com.echoim.server.dto.message.EditMessageRequestDto;
import com.echoim.server.dto.message.ForwardMessageRequestDto;
import com.echoim.server.entity.ImMessageReactionEntity;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsChatSingleData;
import com.echoim.server.im.model.WsGroupChatData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImMessageReactionMapper;
import com.echoim.server.service.config.SystemConfigService;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.message.MessageCommandService;
import com.echoim.server.service.message.MessageViewService;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.message.MessageForwardSourceVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageCommandServiceImpl implements MessageCommandService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_STICKER = 2;
    private static final int MESSAGE_TYPE_IMAGE = 3;
    private static final int MESSAGE_TYPE_GIF = 4;
    private static final int MESSAGE_TYPE_FILE = 5;
    private static final int MESSAGE_TYPE_SYSTEM = 6;
    private static final int MESSAGE_TYPE_VOICE = 7;
    private static final int MESSAGE_STATUS_RECALLED = 3;
    private static final String CONFIG_KEY_RECALL_SECONDS = "message.recall-seconds";
    private static final String SAVED_BIZ_KEY_PREFIX = "saved_";

    private final ImMessageMapper imMessageMapper;
    private final ImMessageReactionMapper imMessageReactionMapper;
    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImSingleChatService imSingleChatService;
    private final ImGroupChatService imGroupChatService;
    private final ImWsPushService imWsPushService;
    private final FileService fileService;
    private final MessageViewService messageViewService;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public MessageCommandServiceImpl(ImMessageMapper imMessageMapper,
                                     ImMessageReactionMapper imMessageReactionMapper,
                                     ImConversationMapper imConversationMapper,
                                     ImConversationUserMapper imConversationUserMapper,
                                     ImSingleChatService imSingleChatService,
                                     ImGroupChatService imGroupChatService,
                                     ImWsPushService imWsPushService,
                                     FileService fileService,
                                     MessageViewService messageViewService,
                                     SystemConfigService systemConfigService,
                                     AuditLogService auditLogService,
                                     ObjectMapper objectMapper) {
        this.imMessageMapper = imMessageMapper;
        this.imMessageReactionMapper = imMessageReactionMapper;
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imSingleChatService = imSingleChatService;
        this.imGroupChatService = imGroupChatService;
        this.imWsPushService = imWsPushService;
        this.fileService = fileService;
        this.messageViewService = messageViewService;
        this.systemConfigService = systemConfigService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> recall(Long userId, Long messageId) {
        ImMessageEntity message = requireOwnedMessage(userId, messageId);
        validateRecall(message);
        Map<String, Object> extraMap = parseExtra(message.getExtraJson());
        extraMap.put("recalled", true);
        extraMap.put("recalledAt", LocalDateTime.now());
        extraMap.put("recalledBy", userId);
        message.setSendStatus(MESSAGE_STATUS_RECALLED);
        message.setExtraJson(toJson(extraMap));
        imMessageMapper.updateById(message);
        refreshConversationPreview(message);
        notifyMessageMutation(message, userId, WsMessageType.MESSAGE_RECALL, "MESSAGE_RECALL");
        auditLogService.log("MESSAGE_RECALL", Map.of("userId", userId, "messageId", messageId, "conversationId", message.getConversationId()));
        return Map.of("messageId", messageId, "conversationId", message.getConversationId(), "status", "RECALLED");
    }

    @Override
    @Transactional
    public Map<String, Object> edit(Long userId, Long messageId, EditMessageRequestDto requestDto) {
        ImMessageEntity message = requireOwnedMessage(userId, messageId);
        validateEdit(message, requestDto);
        Map<String, Object> extraMap = parseExtra(message.getExtraJson());
        extraMap.put("edited", true);
        extraMap.put("editedAt", LocalDateTime.now());
        message.setContent(ContentSanitizer.escapeHtml(requestDto.getContent().trim()));
        message.setExtraJson(toJson(extraMap));
        imMessageMapper.updateById(message);
        refreshConversationPreview(message);
        notifyMessageMutation(message, userId, WsMessageType.MESSAGE_EDIT, "MESSAGE_EDIT");
        auditLogService.log("MESSAGE_EDIT", Map.of("userId", userId, "messageId", messageId, "conversationId", message.getConversationId()));
        return Map.of("messageId", messageId, "conversationId", message.getConversationId(), "status", "EDITED");
    }

    @Override
    @Transactional
    public Map<String, Object> forward(Long userId, ForwardMessageRequestDto requestDto) {
        List<Long> sourceMessageIds = distinctNonNull(requestDto.getMessageIds());
        List<Long> targetConversationIds = distinctNonNull(requestDto.getTargetConversationIds());
        if (sourceMessageIds.isEmpty() || targetConversationIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "转发参数错误");
        }
        LoginUser loginUser = buildLoginUser(userId);
        int forwardedCount = 0;
        for (Long sourceMessageId : sourceMessageIds) {
            ImMessageEntity sourceMessage = imMessageMapper.selectAccessibleEntityByIdAndUserId(sourceMessageId, userId);
            if (sourceMessage == null || Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(sourceMessage.getSendStatus())) {
                throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "源消息不存在");
            }
            for (Long targetConversationId : targetConversationIds) {
                ImConversationEntity conversation = requireConversationMember(targetConversationId, userId);
                if (Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(conversation.getConversationType())) {
                    WsMessage wsMessage = buildSingleForwardMessage(loginUser, sourceMessage, conversation);
                    imSingleChatService.sendSingle(loginUser, wsMessage);
                } else if (Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(conversation.getConversationType())
                        || Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(conversation.getConversationType())) {
                    WsMessage wsMessage = buildGroupForwardMessage(sourceMessage, conversation);
                    imGroupChatService.sendGroup(loginUser, wsMessage);
                } else {
                    throw new BizException(ErrorCode.PARAM_ERROR, "不支持的目标会话");
                }
                forwardedCount++;
            }
        }
        auditLogService.log("MESSAGE_FORWARD", Map.of("userId", userId, "messageIds", sourceMessageIds, "targetConversationIds", targetConversationIds, "forwardedCount", forwardedCount));
        return Map.of("forwardedCount", forwardedCount);
    }

    @Override
    @Transactional
    public MessageItemVo toggleReaction(Long userId, Long messageId, String emoji) {
        String normalizedEmoji = emoji == null ? "" : emoji.trim();
        if (normalizedEmoji.isEmpty() || normalizedEmoji.length() > 16) {
            throw new BizException(ErrorCode.PARAM_ERROR, "表情参数错误");
        }
        ImMessageEntity message = imMessageMapper.selectAccessibleEntityByIdAndUserId(messageId, userId);
        if (message == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "消息不存在");
        }
        if (Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(message.getSendStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "已撤回消息不能添加反应");
        }

        ImMessageReactionEntity existing = imMessageReactionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImMessageReactionEntity>()
                        .eq(ImMessageReactionEntity::getMessageId, messageId)
                        .eq(ImMessageReactionEntity::getUserId, userId)
                        .last("LIMIT 1"));
        if (existing != null && normalizedEmoji.equals(existing.getEmoji())) {
            imMessageReactionMapper.deleteById(existing.getId());
        } else if (existing != null) {
            existing.setEmoji(normalizedEmoji);
            imMessageReactionMapper.updateById(existing);
        } else {
            ImMessageReactionEntity reaction = new ImMessageReactionEntity();
            reaction.setMessageId(messageId);
            reaction.setUserId(userId);
            reaction.setEmoji(normalizedEmoji);
            imMessageReactionMapper.insert(reaction);
        }

        auditLogService.log("MESSAGE_REACTION", Map.of("userId", userId, "messageId", messageId, "emoji", normalizedEmoji));
        notifyMessageMutation(message, userId, null, "MESSAGE_REACTION");
        return buildMessageItem(message, userId);
    }

    private ImMessageEntity requireOwnedMessage(Long userId, Long messageId) {
        ImMessageEntity message = imMessageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getFromUserId(), userId)) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "消息不存在");
        }
        requireConversationMember(message.getConversationId(), userId);
        return message;
    }

    private void validateRecall(ImMessageEntity message) {
        if (Integer.valueOf(MESSAGE_TYPE_SYSTEM).equals(message.getMsgType())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "系统消息不支持撤回");
        }
        if (Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(message.getSendStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "消息已撤回");
        }
        validateRecallWindow(message);
    }

    private void validateEdit(ImMessageEntity message, EditMessageRequestDto requestDto) {
        if (!StringUtils.hasText(requestDto.getContent())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (!Integer.valueOf(MESSAGE_TYPE_TEXT).equals(message.getMsgType())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "仅文本消息支持编辑");
        }
        if (Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(message.getSendStatus())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "已撤回消息不能编辑");
        }
        validateRecallWindow(message);
    }

    private void validateRecallWindow(ImMessageEntity message) {
        int recallSeconds = systemConfigService.getIntValue(CONFIG_KEY_RECALL_SECONDS, 120);
        if (message.getSentAt() == null || message.getSentAt().plusSeconds(recallSeconds).isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "已超过可操作时间窗口");
        }
    }

    private ImConversationEntity requireConversationMember(Long conversationId, Long userId) {
        ImConversationEntity conversation = imConversationMapper.selectById(conversationId);
        ImConversationUserEntity conversationUser = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (conversation == null || conversationUser == null || !Integer.valueOf(1).equals(conversation.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private WsMessage buildSingleForwardMessage(LoginUser loginUser, ImMessageEntity sourceMessage, ImConversationEntity conversation) {
        Long toUserId = isSavedMessagesConversation(conversation)
                ? loginUser.getUserId()
                : imConversationUserMapper.selectByConversationId(conversation.getId()).stream()
                        .map(ImConversationUserEntity::getUserId)
                        .filter(memberId -> !memberId.equals(loginUser.getUserId()))
                        .findFirst()
                        .orElseThrow(() -> new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "目标单聊不存在"));
        WsChatSingleData data = new WsChatSingleData();
        data.setConversationId(conversation.getId());
        data.setToUserId(toUserId);
        data.setMsgType(resolveWsMsgType(sourceMessage.getMsgType()));
        data.setContent(sourceMessage.getContent());
        data.setFileId(sourceMessage.getFileId());
        data.setExtraJson(buildForwardExtra(sourceMessage));

        WsMessage wsMessage = new WsMessage();
        wsMessage.setType(WsMessageType.CHAT_SINGLE);
        wsMessage.setClientMsgId("forward-" + sourceMessage.getId() + "-" + conversation.getId() + "-" + System.nanoTime());
        wsMessage.setTimestamp(System.currentTimeMillis());
        wsMessage.setData(data);
        return wsMessage;
    }

    private WsMessage buildGroupForwardMessage(ImMessageEntity sourceMessage, ImConversationEntity conversation) {
        WsGroupChatData data = new WsGroupChatData();
        data.setConversationId(conversation.getId());
        data.setGroupId(conversation.getBizId());
        data.setMsgType(resolveWsMsgType(sourceMessage.getMsgType()));
        data.setContent(sourceMessage.getContent());
        data.setFileId(sourceMessage.getFileId());
        data.setExtraJson(buildForwardExtra(sourceMessage));

        WsMessage wsMessage = new WsMessage();
        wsMessage.setType(WsMessageType.CHAT_GROUP);
        wsMessage.setClientMsgId("forward-" + sourceMessage.getId() + "-" + conversation.getId() + "-" + System.nanoTime());
        wsMessage.setTimestamp(System.currentTimeMillis());
        wsMessage.setData(data);
        return wsMessage;
    }

    private Map<String, Object> buildForwardExtra(ImMessageEntity sourceMessage) {
        Map<String, Object> extraMap = parseExtra(sourceMessage.getExtraJson());
        MessageForwardSourceVo forwardSource = new MessageForwardSourceVo();
        forwardSource.setSourceMessageId(sourceMessage.getId());
        forwardSource.setSourceConversationId(sourceMessage.getConversationId());
        forwardSource.setSourceSenderId(sourceMessage.getFromUserId());
        forwardSource.setSourceMsgType(resolveWsMsgType(sourceMessage.getMsgType()));
        forwardSource.setSourcePreview(buildPreview(sourceMessage));
        extraMap.put("forwardSource", forwardSource);
        return extraMap;
    }

    private String buildPreview(ImMessageEntity sourceMessage) {
        return switch (sourceMessage.getMsgType()) {
            case MESSAGE_TYPE_STICKER -> "[贴纸]";
            case MESSAGE_TYPE_IMAGE -> "[图片]";
            case MESSAGE_TYPE_GIF -> "[GIF]";
            case MESSAGE_TYPE_FILE -> "[文件]";
            case MESSAGE_TYPE_VOICE -> "[语音]";
            default -> sourceMessage.getContent();
        };
    }

    private String resolveWsMsgType(Integer msgType) {
        return switch (msgType) {
            case MESSAGE_TYPE_STICKER -> "STICKER";
            case MESSAGE_TYPE_IMAGE -> "IMAGE";
            case MESSAGE_TYPE_GIF -> "GIF";
            case MESSAGE_TYPE_FILE -> "FILE";
            case MESSAGE_TYPE_SYSTEM -> "SYSTEM";
            case MESSAGE_TYPE_VOICE -> "VOICE";
            default -> "TEXT";
        };
    }

    private void refreshConversationPreview(ImMessageEntity message) {
        ImConversationEntity conversation = imConversationMapper.selectById(message.getConversationId());
        if (conversation == null || !Objects.equals(conversation.getLastMessageId(), message.getId())) {
            return;
        }
        String preview = Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(message.getSendStatus())
                ? "撤回了一条消息"
                : buildPreview(message);
        imConversationMapper.updateLastMessageState(conversation.getId(), message.getId(), preview, message.getSentAt());
    }

    private void notifyMessageMutation(ImMessageEntity message, Long operatorUserId, WsMessageType type, String changeType) {
        List<ImConversationUserEntity> members = imConversationUserMapper.selectByConversationId(message.getConversationId());
        for (ImConversationUserEntity member : members) {
            if (member.getDeleted() != null
                    && member.getDeleted() == 1
                    && (Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(message.getConversationType())
                    || Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(message.getConversationType()))) {
                continue;
            }
            WsMessageItem item = toWsMessageItem(message, member.getUserId());
            if (type != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("message", item);
                imWsPushService.pushToUser(member.getUserId(), type, null, message.getClientMsgId(), payload);
            }
            var conversationItem = imConversationMapper.selectConversationItemByUserId(message.getConversationId(), member.getUserId());
            if (conversationItem != null) {
                imWsPushService.pushConversationChange(member.getUserId(), changeType, conversationItem, item);
            }
        }
    }

    private WsMessageItem toWsMessageItem(ImMessageEntity entity, Long viewerUserId) {
        WsMessageItem item = new WsMessageItem();
        item.setMessageId(entity.getId());
        item.setConversationId(entity.getConversationId());
        item.setConversationType(entity.getConversationType());
        item.setSeqNo(entity.getSeqNo());
        item.setClientMsgId(entity.getClientMsgId());
        item.setFromUserId(entity.getFromUserId());
        item.setToUserId(entity.getToUserId());
        item.setGroupId(entity.getGroupId());
        item.setMsgType(resolveWsMsgType(entity.getMsgType()));
        item.setContent(entity.getContent());
        item.setFileId(entity.getFileId());
        item.setSendStatus(entity.getSendStatus());
        item.setSentAt(entity.getSentAt());
        fileService.enrichWsMessage(viewerUserId, item);
        messageViewService.enrichWsMessage(viewerUserId, item, entity);
        return item;
    }

    private MessageItemVo buildMessageItem(ImMessageEntity entity, Long viewerUserId) {
        MessageItemVo item = new MessageItemVo();
        item.setMessageId(entity.getId());
        item.setConversationId(entity.getConversationId());
        item.setConversationType(entity.getConversationType());
        item.setSeqNo(entity.getSeqNo());
        item.setClientMsgId(entity.getClientMsgId());
        item.setFromUserId(entity.getFromUserId());
        item.setToUserId(entity.getToUserId());
        item.setGroupId(entity.getGroupId());
        item.setMsgType(resolveWsMsgType(entity.getMsgType()));
        item.setContent(entity.getContent());
        item.setFileId(entity.getFileId());
        item.setSendStatus(entity.getSendStatus());
        item.setSentAt(entity.getSentAt());
        fileService.enrichMessages(viewerUserId, List.of(item));
        messageViewService.enrichMessages(viewerUserId, List.of(item));
        return item;
    }

    private LoginUser buildLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setTokenType("user");
        return loginUser;
    }

    private boolean isSavedMessagesConversation(ImConversationEntity conversation) {
        return conversation.getBizKey() != null && conversation.getBizKey().startsWith(SAVED_BIZ_KEY_PREFIX);
    }

    private List<Long> distinctNonNull(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).distinct().toList();
    }

    private Map<String, Object> parseExtra(String extraJson) {
        if (!StringUtils.hasText(extraJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(extraJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Map<String, Object> extraMap) {
        try {
            return objectMapper.writeValueAsString(extraMap);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "消息扩展字段格式错误");
        }
    }
}
