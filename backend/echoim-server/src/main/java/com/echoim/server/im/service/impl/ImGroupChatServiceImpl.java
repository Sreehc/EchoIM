package com.echoim.server.im.service.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.entity.ImGroupEntity;
import com.echoim.server.entity.ImGroupMemberEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsGroupChatData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.common.ratelimit.LocalRateLimitService;
import com.echoim.server.common.util.ContentSanitizer;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImGroupMapper;
import com.echoim.server.mapper.ImGroupMemberMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.block.BlockService;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.message.StickerCatalog;
import com.echoim.server.service.message.MessageViewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImGroupChatServiceImpl implements ImGroupChatService {

    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int GROUP_STATUS_NORMAL = 1;
    private static final int MEMBER_ROLE_OWNER = 1;
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_STICKER = 2;
    private static final int MESSAGE_TYPE_IMAGE = 3;
    private static final int MESSAGE_TYPE_GIF = 4;
    private static final int MESSAGE_TYPE_FILE = 5;
    private static final int MESSAGE_TYPE_VOICE = 7;
    private static final int FILE_BIZ_TYPE_IMAGE = 2;
    private static final int FILE_BIZ_TYPE_FILE = 4;
    private static final int FILE_BIZ_TYPE_AUDIO = 5;
    private static final int MESSAGE_STATUS_SENT = 1;
    private static final String ACK_TYPE_SEND = "SEND";
    private static final String CHANGE_TYPE_MESSAGE_NEW = "MESSAGE_NEW";

    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImGroupMapper imGroupMapper;
    private final ImGroupMemberMapper imGroupMemberMapper;
    private final ImWsPushService imWsPushService;
    private final BlockService blockService;
    private final FileService fileService;
    private final StickerCatalog stickerCatalog;
    private final MessageViewService messageViewService;
    private final LocalRateLimitService localRateLimitService;
    private final ObjectMapper objectMapper;

    public ImGroupChatServiceImpl(ImConversationMapper imConversationMapper,
                                  ImConversationUserMapper imConversationUserMapper,
                                  ImMessageMapper imMessageMapper,
                                  ImGroupMapper imGroupMapper,
                                  ImGroupMemberMapper imGroupMemberMapper,
                                  ImWsPushService imWsPushService,
                                  BlockService blockService,
                                  FileService fileService,
                                  StickerCatalog stickerCatalog,
                                  MessageViewService messageViewService,
                                  LocalRateLimitService localRateLimitService,
                                  ObjectMapper objectMapper) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imGroupMapper = imGroupMapper;
        this.imGroupMemberMapper = imGroupMemberMapper;
        this.imWsPushService = imWsPushService;
        this.blockService = blockService;
        this.fileService = fileService;
        this.stickerCatalog = stickerCatalog;
        this.messageViewService = messageViewService;
        this.localRateLimitService = localRateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> sendGroup(LoginUser loginUser, WsMessage message) {
        localRateLimitService.check("ws-group-send:" + loginUser.getUserId(), 120, 60, "发送过于频繁");
        WsGroupChatData data = objectMapper.convertValue(message.getData(), WsGroupChatData.class);
        validateChatRequest(message, data);

        ImMessageEntity duplicate = imMessageMapper.selectByFromUserIdAndClientMsgId(loginUser.getUserId(), message.getClientMsgId());
        if (duplicate != null) {
            WsMessageItem duplicateItem = toWsMessageItem(duplicate, loginUser.getUserId());
            fileService.enrichWsMessage(loginUser.getUserId(), duplicateItem);
            return sendAckData(duplicateItem, true);
        }

        ImConversationEntity conversation = imConversationMapper.selectByIdForUpdate(data.getConversationId());
        validateGroupConversation(conversation, data.getGroupId());
        validateGroup(data.getGroupId());
        ImGroupMemberEntity currentMember = requireActiveMember(data.getGroupId(), loginUser.getUserId());
        validateNotMuted(currentMember);
        validateSendPermission(conversation, currentMember);
        ImFileEntity messageFile = validateAndLoadMessageFile(loginUser.getUserId(), data);

        ImMessageEntity entity = buildMessage(loginUser.getUserId(), data, message.getClientMsgId(), conversation.getConversationType());
        entity.setSeqNo(nextSeqNo(conversation.getId()));
        try {
            imMessageMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            ImMessageEntity existing = imMessageMapper.selectByFromUserIdAndClientMsgId(loginUser.getUserId(), message.getClientMsgId());
            if (existing != null) {
                WsMessageItem existingItem = toWsMessageItem(existing, loginUser.getUserId());
                fileService.enrichWsMessage(loginUser.getUserId(), existingItem);
                return sendAckData(existingItem, true);
            }
            throw ex;
        }

        imConversationUserMapper.resetDeleted(conversation.getId(), loginUser.getUserId());
        List<Long> senderBlockedIds = blockService.getBlockedUserIds(loginUser.getUserId());
        List<Long> recipientUserIds = imGroupMemberMapper.selectActiveUserIdsByGroupId(data.getGroupId())
                .stream()
                .filter(userId -> !userId.equals(loginUser.getUserId()))
                .filter(userId -> !senderBlockedIds.contains(userId))
                .filter(userId -> !blockService.isBlocked(userId, loginUser.getUserId()))
                .toList();
        if (!recipientUserIds.isEmpty()) {
            imConversationUserMapper.incrementUnreadBatch(conversation.getId(), recipientUserIds);
        }
        imConversationMapper.updateLastMessage(conversation.getId(), entity.getId(), preview(entity, messageFile));

        List<Long> mentionedUserIds = extractMentionedUserIds(entity.getExtraJson());

        WsMessageItem senderItem = toWsMessageItem(entity, loginUser.getUserId());
        fileService.enrichWsMessage(loginUser.getUserId(), senderItem);
        pushConversationChange(loginUser.getUserId(), conversation.getId(), senderItem);
        for (Long recipientUserId : recipientUserIds) {
            WsMessageItem recipientItem = toWsMessageItem(entity, recipientUserId);
            fileService.enrichWsMessage(recipientUserId, recipientItem);
            pushGroupMessage(recipientUserId, message, recipientItem);
            boolean isMentioned = mentionedUserIds.contains(recipientUserId);
            pushConversationChange(recipientUserId, conversation.getId(), recipientItem, isMentioned);
        }
        return sendAckData(senderItem, false);
    }

    private void validateChatRequest(WsMessage message, WsGroupChatData data) {
        if (!StringUtils.hasText(message.getClientMsgId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "clientMsgId 不能为空");
        }
        if (data == null || data.getConversationId() == null || data.getGroupId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "群聊消息参数错误");
        }
        int msgType = toMessageType(data.getMsgType());
        if (msgType == MESSAGE_TYPE_TEXT && !StringUtils.hasText(data.getContent())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文本消息内容不能为空");
        }
        if ((msgType == MESSAGE_TYPE_IMAGE || msgType == MESSAGE_TYPE_GIF || msgType == MESSAGE_TYPE_FILE || msgType == MESSAGE_TYPE_VOICE) && data.getFileId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件消息必须传 fileId");
        }
        if (msgType == MESSAGE_TYPE_VOICE) {
            validateVoiceExtra(data);
        }
        if (msgType == MESSAGE_TYPE_STICKER) {
            normalizeStickerExtra(data);
        }
    }

    private void validateGroupConversation(ImConversationEntity conversation, Long groupId) {
        if (conversation == null || !Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(conversation.getStatus())
                || (!Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(conversation.getConversationType())
                && !Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(conversation.getConversationType()))
                || !groupId.equals(conversation.getBizId())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组或频道会话不存在");
        }
    }

    private void validateGroup(Long groupId) {
        ImGroupEntity group = imGroupMapper.selectById(groupId);
        if (group == null || !Integer.valueOf(GROUP_STATUS_NORMAL).equals(group.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组不存在");
        }
    }

    private ImGroupMemberEntity requireActiveMember(Long groupId, Long userId) {
        ImGroupMemberEntity member = imGroupMemberMapper.selectActiveByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "不是有效成员，无法发送消息");
        }
        return member;
    }

    private void validateNotMuted(ImGroupMemberEntity member) {
        if (member.getMuteUntil() != null && member.getMuteUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.MEMBER_MUTED, "你已被禁言，无法发送消息");
        }
    }

    private void validateSendPermission(ImConversationEntity conversation, ImGroupMemberEntity member) {
        if (Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(conversation.getConversationType())
                && !Integer.valueOf(MEMBER_ROLE_OWNER).equals(member.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "频道仅创建者可发送消息");
        }
    }

    private ImMessageEntity buildMessage(Long fromUserId, WsGroupChatData data, String clientMsgId, Integer conversationType) {
        int msgType = toMessageType(data.getMsgType());
        ImMessageEntity entity = new ImMessageEntity();
        entity.setConversationId(data.getConversationId());
        entity.setConversationType(conversationType);
        entity.setClientMsgId(clientMsgId);
        entity.setFromUserId(fromUserId);
        entity.setGroupId(data.getGroupId());
        entity.setMsgType(msgType);
        entity.setContent(ContentSanitizer.isTextType(msgType)
                ? ContentSanitizer.escapeHtml(data.getContent())
                : data.getContent());
        entity.setExtraJson(toJson(data.getExtraJson()));
        entity.setFileId(data.getFileId());
        entity.setSendStatus(MESSAGE_STATUS_SENT);
        entity.setSentAt(LocalDateTime.now());
        return entity;
    }

    private int toMessageType(String msgType) {
        if (!StringUtils.hasText(msgType) || "TEXT".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_TEXT;
        }
        if ("STICKER".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_STICKER;
        }
        if ("IMAGE".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_IMAGE;
        }
        if ("GIF".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_GIF;
        }
        if ("FILE".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_FILE;
        }
        if ("VOICE".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_VOICE;
        }
        throw new BizException(ErrorCode.PARAM_ERROR, "不支持的消息类型");
    }

    private String toExternalMsgType(Integer msgType) {
        if (Integer.valueOf(MESSAGE_TYPE_TEXT).equals(msgType)) {
            return "TEXT";
        }
        if (Integer.valueOf(MESSAGE_TYPE_STICKER).equals(msgType)) {
            return "STICKER";
        }
        if (Integer.valueOf(MESSAGE_TYPE_IMAGE).equals(msgType)) {
            return "IMAGE";
        }
        if (Integer.valueOf(MESSAGE_TYPE_GIF).equals(msgType)) {
            return "GIF";
        }
        if (Integer.valueOf(MESSAGE_TYPE_FILE).equals(msgType)) {
            return "FILE";
        }
        if (Integer.valueOf(MESSAGE_TYPE_VOICE).equals(msgType)) {
            return "VOICE";
        }
        return "UNKNOWN";
    }

    private ImFileEntity validateAndLoadMessageFile(Long userId, WsGroupChatData data) {
        int msgType = toMessageType(data.getMsgType());
        if (msgType == MESSAGE_TYPE_IMAGE || msgType == MESSAGE_TYPE_GIF) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_IMAGE);
        }
        if (msgType == MESSAGE_TYPE_FILE) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_FILE);
        }
        if (msgType == MESSAGE_TYPE_VOICE) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_AUDIO);
        }
        return null;
    }

    private void normalizeStickerExtra(WsGroupChatData data) {
        Object extra = data.getExtraJson();
        if (!(extra instanceof Map<?, ?> rawMap)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "贴纸参数错误");
        }
        Map<String, Object> extraMap = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> extraMap.put(String.valueOf(key), value));
        Object stickerObject = extraMap.get("sticker");
        if (!(stickerObject instanceof Map<?, ?> stickerMap)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "贴纸参数错误");
        }
        Object stickerId = stickerMap.get("stickerId");
        extraMap.put("sticker", stickerCatalog.require(stickerId == null ? null : String.valueOf(stickerId)));
        data.setExtraJson(extraMap);
    }

    @SuppressWarnings("unchecked")
    private void validateVoiceExtra(WsGroupChatData data) {
        Object extra = data.getExtraJson();
        if (extra instanceof Map<?, ?> rawMap) {
            Map<String, Object> extraMap = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> extraMap.put(String.valueOf(key), value));
            Object duration = extraMap.get("duration");
            if (duration != null) {
                double durationVal = ((Number) duration).doubleValue();
                if (durationVal < 0 || durationVal > 60) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "语音时长必须在0-60秒之间");
                }
                extraMap.put("duration", durationVal);
            }
            data.setExtraJson(extraMap);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "extraJson 格式错误");
        }
    }

    private long nextSeqNo(Long conversationId) {
        Long current = imMessageMapper.selectMaxSeqNoByConversationId(conversationId);
        return current == null ? 1L : current + 1L;
    }

    private String preview(ImMessageEntity entity, ImFileEntity messageFile) {
        String preview;
        if (Integer.valueOf(MESSAGE_TYPE_STICKER).equals(entity.getMsgType())) {
            preview = "[贴纸]";
        } else if (Integer.valueOf(MESSAGE_TYPE_GIF).equals(entity.getMsgType())) {
            preview = "[GIF]";
        } else if (Integer.valueOf(MESSAGE_TYPE_IMAGE).equals(entity.getMsgType())) {
            preview = "[图片]";
        } else if (Integer.valueOf(MESSAGE_TYPE_FILE).equals(entity.getMsgType())) {
            preview = "[文件] " + (messageFile == null ? "" : messageFile.getFileName());
        } else if (Integer.valueOf(MESSAGE_TYPE_VOICE).equals(entity.getMsgType())) {
            preview = "[语音]";
        } else {
            preview = entity.getContent();
        }
        if (preview == null) {
            return "";
        }
        return preview.length() > 500 ? preview.substring(0, 500) : preview;
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
        item.setMsgType(toExternalMsgType(entity.getMsgType()));
        item.setContent(entity.getContent());
        item.setFileId(entity.getFileId());
        item.setSendStatus(entity.getSendStatus());
        item.setSentAt(entity.getSentAt());
        messageViewService.enrichWsMessage(viewerUserId, item, entity);
        return item;
    }

    private Map<String, Object> sendAckData(WsMessageItem item, boolean duplicate) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ackType", ACK_TYPE_SEND);
        data.put("status", "SUCCESS");
        data.put("duplicate", duplicate);
        data.put("message", item);
        return data;
    }

    private void pushGroupMessage(Long toUserId, WsMessage request, WsMessageItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", item);
        imWsPushService.pushToUser(toUserId, WsMessageType.CHAT_GROUP, request.getTraceId(), request.getClientMsgId(), data);
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractMentionedUserIds(String extraJson) {
        if (extraJson == null || extraJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> extra = objectMapper.readValue(extraJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Object mentionsObj = extra.get("mentions");
            if (!(mentionsObj instanceof List<?> mentionsList)) {
                return Collections.emptyList();
            }
            return mentionsList.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> {
                        Map<String, Object> map = (Map<String, Object>) item;
                        Object userId = map.get("userId");
                        return userId instanceof Number ? ((Number) userId).longValue() : null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void pushConversationChange(Long userId, Long conversationId, Object message) {
        pushConversationChange(userId, conversationId, message, false);
    }

    private void pushConversationChange(Long userId, Long conversationId, Object message, boolean isMentioned) {
        var conversation = imConversationMapper.selectConversationItemByUserId(conversationId, userId);
        if (conversation != null) {
            java.util.List<Long> atUserIds = isMentioned ? List.of(userId) : null;
            imWsPushService.pushConversationChange(userId, CHANGE_TYPE_MESSAGE_NEW, conversation, message, atUserIds);
        }
    }
}
