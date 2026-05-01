package com.echoim.server.im.service.impl;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsAckData;
import com.echoim.server.im.model.WsChatSingleData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.model.WsReadData;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.im.service.ImWsPushService;
import com.echoim.server.common.ratelimit.LocalRateLimitService;
import com.echoim.server.common.util.ContentSanitizer;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImMessageReceiptMapper;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.friend.FriendService;
import com.echoim.server.service.message.StickerCatalog;
import com.echoim.server.service.message.MessageViewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@Service
public class ImSingleChatServiceImpl implements ImSingleChatService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_STICKER = 2;
    private static final int MESSAGE_TYPE_IMAGE = 3;
    private static final int MESSAGE_TYPE_GIF = 4;
    private static final int MESSAGE_TYPE_FILE = 5;
    private static final int FILE_BIZ_TYPE_IMAGE = 2;
    private static final int FILE_BIZ_TYPE_FILE = 4;
    private static final int MESSAGE_STATUS_SENT = 1;
    private static final int RECEIPT_TYPE_DELIVERED = 1;
    private static final int RECEIPT_TYPE_READ = 2;
    private static final String ACK_TYPE_SEND = "SEND";
    private static final String ACK_TYPE_DELIVERED = "DELIVERED";
    private static final String CHANGE_TYPE_MESSAGE_NEW = "MESSAGE_NEW";
    private static final String CHANGE_TYPE_READ_UPDATE = "READ_UPDATE";
    private static final String SAVED_BIZ_KEY_PREFIX = "saved_";

    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImMessageReceiptMapper imMessageReceiptMapper;
    private final ImWsPushService imWsPushService;
    private final FriendService friendService;
    private final FileService fileService;
    private final StickerCatalog stickerCatalog;
    private final MessageViewService messageViewService;
    private final LocalRateLimitService localRateLimitService;
    private final ObjectMapper objectMapper;

    public ImSingleChatServiceImpl(ImConversationMapper imConversationMapper,
                                   ImConversationUserMapper imConversationUserMapper,
                                   ImMessageMapper imMessageMapper,
                                   ImMessageReceiptMapper imMessageReceiptMapper,
                                   ImWsPushService imWsPushService,
                                   FriendService friendService,
                                   FileService fileService,
                                   StickerCatalog stickerCatalog,
                                   MessageViewService messageViewService,
                                   LocalRateLimitService localRateLimitService,
                                   ObjectMapper objectMapper) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imMessageReceiptMapper = imMessageReceiptMapper;
        this.imWsPushService = imWsPushService;
        this.friendService = friendService;
        this.fileService = fileService;
        this.stickerCatalog = stickerCatalog;
        this.messageViewService = messageViewService;
        this.localRateLimitService = localRateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> sendSingle(LoginUser loginUser, WsMessage message) {
        localRateLimitService.check("ws-single-send:" + loginUser.getUserId(), 120, 60, "发送过于频繁");
        WsChatSingleData data = objectMapper.convertValue(message.getData(), WsChatSingleData.class);
        validateChatRequest(message, data);

        ImMessageEntity duplicate = imMessageMapper.selectByFromUserIdAndClientMsgId(loginUser.getUserId(), message.getClientMsgId());
        if (duplicate != null) {
            WsMessageItem duplicateItem = toWsMessageItem(duplicate, loginUser.getUserId());
            fileService.enrichWsMessage(loginUser.getUserId(), duplicateItem);
            return sendAckData(duplicateItem, true);
        }

        ImConversationEntity conversation = imConversationMapper.selectByIdForUpdate(data.getConversationId());
        validateSingleConversation(conversation);
        ImConversationUserEntity senderMember = requireMember(conversation.getId(), loginUser.getUserId());
        ImConversationUserEntity receiverMember = requireMember(conversation.getId(), data.getToUserId());
        boolean savedMessagesConversation = isSavedMessagesConversation(conversation);
        if (!savedMessagesConversation && senderMember.getUserId().equals(receiverMember.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能给自己发送单聊消息");
        }
        if (!savedMessagesConversation) {
            friendService.validateSingleChatAllowed(loginUser.getUserId(), data.getToUserId());
        }
        ImFileEntity messageFile = validateAndLoadMessageFile(loginUser.getUserId(), data);

        ImMessageEntity entity = buildMessage(loginUser.getUserId(), data, message.getClientMsgId());
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
        imConversationMapper.updateLastMessage(conversation.getId(), entity.getId(), preview(entity, messageFile));

        WsMessageItem senderItem = toWsMessageItem(entity, loginUser.getUserId());
        fileService.enrichWsMessage(loginUser.getUserId(), senderItem);
        pushConversationChange(loginUser.getUserId(), conversation.getId(), CHANGE_TYPE_MESSAGE_NEW, senderItem);
        if (!savedMessagesConversation) {
            imConversationUserMapper.incrementUnread(conversation.getId(), data.getToUserId());
            WsMessageItem receiverItem = toWsMessageItem(entity, data.getToUserId());
            fileService.enrichWsMessage(data.getToUserId(), receiverItem);
            pushSingleMessage(data.getToUserId(), message, receiverItem);
            pushConversationChange(data.getToUserId(), conversation.getId(), CHANGE_TYPE_MESSAGE_NEW, receiverItem);
        }
        return sendAckData(senderItem, false);
    }

    @Override
    @Transactional
    public Map<String, Object> deliveredAck(LoginUser loginUser, WsMessage message) {
        WsAckData data = objectMapper.convertValue(message.getData(), WsAckData.class);
        if (data == null || !ACK_TYPE_DELIVERED.equalsIgnoreCase(data.getAckType())
                || data.getConversationId() == null || data.getMessageId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "ACK 参数错误");
        }

        ImMessageEntity entity = imMessageMapper.selectById(data.getMessageId());
        validateAckMessage(loginUser.getUserId(), data, entity);
        imMessageReceiptMapper.insertIgnore(entity.getId(), entity.getConversationId(), loginUser.getUserId(), RECEIPT_TYPE_DELIVERED);

        Map<String, Object> ackData = deliveredAckData(entity, loginUser.getUserId());
        imWsPushService.pushToUser(entity.getFromUserId(), WsMessageType.ACK, message.getTraceId(), message.getClientMsgId(), ackData);
        return ackData;
    }

    @Override
    @Transactional
    public Map<String, Object> read(LoginUser loginUser, WsMessage message) {
        WsReadData data = objectMapper.convertValue(message.getData(), WsReadData.class);
        if (data == null || data.getConversationId() == null || data.getLastReadSeq() == null || data.getLastReadSeq() < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "READ 参数错误");
        }
        return read(loginUser.getUserId(), data.getConversationId(), data.getLastReadSeq(), message.getTraceId(), message.getClientMsgId());
    }

    @Override
    @Transactional
    public Map<String, Object> read(Long userId, Long conversationId, Long lastReadSeq, String traceId, String clientMsgId) {
        if (conversationId == null || lastReadSeq == null || lastReadSeq < 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "READ 参数错误");
        }
        ImConversationEntity conversation = imConversationMapper.selectByIdForUpdate(conversationId);
        validateConversationForRead(conversation);
        ImConversationUserEntity member = requireMember(conversation.getId(), userId);
        long previousLastReadSeq = member.getLastReadSeq() == null ? 0L : member.getLastReadSeq();
        long effectiveLastReadSeq = Math.max(member.getLastReadSeq() == null ? 0L : member.getLastReadSeq(), lastReadSeq);

        imConversationUserMapper.updateReadState(conversation.getId(), userId, effectiveLastReadSeq);

        Map<String, Object> readData = readData(conversation.getId(), userId, effectiveLastReadSeq);
        if (Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(conversation.getConversationType())) {
            imMessageReceiptMapper.insertReadReceiptsUpToSeq(conversation.getId(), userId, effectiveLastReadSeq, RECEIPT_TYPE_READ);
            otherMemberId(conversation.getId(), userId)
                    .ifPresent(targetUserId -> imWsPushService.pushToUser(targetUserId, WsMessageType.READ, traceId, clientMsgId, readData));
        } else if (Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(conversation.getConversationType())
                && effectiveLastReadSeq > previousLastReadSeq) {
            pushChannelReadUpdates(conversation, userId, previousLastReadSeq, effectiveLastReadSeq);
        }
        pushConversationChange(userId, conversation.getId(), CHANGE_TYPE_READ_UPDATE, null);
        return readData;
    }

    private void validateChatRequest(WsMessage message, WsChatSingleData data) {
        if (!StringUtils.hasText(message.getClientMsgId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "clientMsgId 不能为空");
        }
        if (data == null || data.getConversationId() == null || data.getToUserId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单聊消息参数错误");
        }
        int msgType = toMessageType(data.getMsgType());
        if (msgType == MESSAGE_TYPE_TEXT && !StringUtils.hasText(data.getContent())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文本消息内容不能为空");
        }
        if ((msgType == MESSAGE_TYPE_IMAGE || msgType == MESSAGE_TYPE_GIF || msgType == MESSAGE_TYPE_FILE) && data.getFileId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件消息必须传 fileId");
        }
        if (msgType == MESSAGE_TYPE_STICKER) {
            normalizeStickerExtra(data);
        }
    }

    private void validateSingleConversation(ImConversationEntity conversation) {
        if (conversation == null || !Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(conversation.getStatus())
                || !Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(conversation.getConversationType())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
    }

    private void validateConversationForRead(ImConversationEntity conversation) {
        if (conversation == null || !Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(conversation.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
        if (!Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(conversation.getConversationType())
                && !Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(conversation.getConversationType())
                && !Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(conversation.getConversationType())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
    }

    private ImConversationUserEntity requireMember(Long conversationId, Long userId) {
        ImConversationUserEntity member = imConversationUserMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }
        return member;
    }

    private ImMessageEntity buildMessage(Long fromUserId, WsChatSingleData data, String clientMsgId) {
        int msgType = toMessageType(data.getMsgType());
        ImMessageEntity entity = new ImMessageEntity();
        entity.setConversationId(data.getConversationId());
        entity.setConversationType(CONVERSATION_TYPE_SINGLE);
        entity.setClientMsgId(clientMsgId);
        entity.setFromUserId(fromUserId);
        entity.setToUserId(data.getToUserId());
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
        return "UNKNOWN";
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

    private ImFileEntity validateAndLoadMessageFile(Long userId, WsChatSingleData data) {
        int msgType = toMessageType(data.getMsgType());
        if (msgType == MESSAGE_TYPE_IMAGE || msgType == MESSAGE_TYPE_GIF) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_IMAGE);
        }
        if (msgType == MESSAGE_TYPE_FILE) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_FILE);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void normalizeStickerExtra(WsChatSingleData data) {
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
        } else {
            preview = entity.getContent();
        }
        if (preview == null) {
            return "";
        }
        return preview.length() > 500 ? preview.substring(0, 500) : preview;
    }

    private void validateAckMessage(Long currentUserId, WsAckData data, ImMessageEntity entity) {
        if (entity == null || !data.getConversationId().equals(entity.getConversationId())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "消息不存在");
        }
        if (!Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(entity.getConversationType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "群聊暂不支持送达 ACK");
        }
        if (data.getSeqNo() != null && !data.getSeqNo().equals(entity.getSeqNo())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "ACK 消息序号错误");
        }
        requireMember(entity.getConversationId(), currentUserId);
        if (currentUserId.equals(entity.getFromUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "发送方不能确认自己的消息送达");
        }
    }

    private Optional<Long> otherMemberId(Long conversationId, Long currentUserId) {
        List<ImConversationUserEntity> members = imConversationUserMapper.selectByConversationId(conversationId);
        return members.stream()
                .map(ImConversationUserEntity::getUserId)
                .filter(userId -> !userId.equals(currentUserId))
                .findFirst();
    }

    private Map<String, Object> sendAckData(ImMessageEntity entity, boolean duplicate) {
        return sendAckData(toWsMessageItem(entity, entity.getFromUserId()), duplicate);
    }

    private Map<String, Object> sendAckData(WsMessageItem item, boolean duplicate) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ackType", ACK_TYPE_SEND);
        data.put("status", "SUCCESS");
        data.put("duplicate", duplicate);
        data.put("message", item);
        return data;
    }

    private Map<String, Object> deliveredAckData(ImMessageEntity entity, Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ackType", ACK_TYPE_DELIVERED);
        data.put("status", "SUCCESS");
        data.put("conversationId", entity.getConversationId());
        data.put("messageId", entity.getId());
        data.put("seqNo", entity.getSeqNo());
        data.put("userId", userId);
        return data;
    }

    private Map<String, Object> readData(Long conversationId, Long readerUserId, Long lastReadSeq) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "SUCCESS");
        data.put("conversationId", conversationId);
        data.put("readerUserId", readerUserId);
        data.put("lastReadSeq", lastReadSeq);
        return data;
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

    private void pushChannelReadUpdates(ImConversationEntity conversation,
                                        Long readerUserId,
                                        Long previousLastReadSeq,
                                        Long effectiveLastReadSeq) {
        List<com.echoim.server.vo.conversation.MessageItemVo> affectedMessages =
                imMessageMapper.selectMessageAfterSeqByConversationIdAndUserId(
                        conversation.getId(),
                        readerUserId,
                        previousLastReadSeq,
                        Math.toIntExact(Math.max(1L, effectiveLastReadSeq - previousLastReadSeq))
                );
        if (affectedMessages.isEmpty()) {
            return;
        }

        List<com.echoim.server.vo.conversation.MessageItemVo> creatorMessages = affectedMessages.stream()
                .filter(message -> !readerUserId.equals(message.getFromUserId()))
                .toList();
        if (creatorMessages.isEmpty()) {
            return;
        }

        List<Long> ownerUserIds = creatorMessages.stream()
                .map(com.echoim.server.vo.conversation.MessageItemVo::getFromUserId)
                .filter(userId -> userId != null && userId > 0)
                .distinct()
                .toList();
        for (Long ownerUserId : ownerUserIds) {
            for (com.echoim.server.vo.conversation.MessageItemVo affectedMessage : creatorMessages) {
                ImMessageEntity entity = imMessageMapper.selectById(affectedMessage.getMessageId());
                if (entity == null) {
                    continue;
                }
                WsMessageItem messageItem = toWsMessageItem(entity, ownerUserId);
                pushConversationChange(ownerUserId, conversation.getId(), CHANGE_TYPE_READ_UPDATE, messageItem);
            }
        }
    }

    private void pushSingleMessage(Long toUserId, WsMessage request, WsMessageItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", item);
        imWsPushService.pushToUser(toUserId, WsMessageType.CHAT_SINGLE, request.getTraceId(), request.getClientMsgId(), data);
    }

    private void pushConversationChange(Long userId, Long conversationId, String changeType, Object message) {
        var conversation = imConversationMapper.selectConversationItemByUserId(conversationId, userId);
        if (conversation != null) {
            imWsPushService.pushConversationChange(userId, changeType, conversation, message);
        }
    }

    private boolean isSavedMessagesConversation(ImConversationEntity conversation) {
        return conversation.getBizKey() != null && conversation.getBizKey().startsWith(SAVED_BIZ_KEY_PREFIX);
    }
}
