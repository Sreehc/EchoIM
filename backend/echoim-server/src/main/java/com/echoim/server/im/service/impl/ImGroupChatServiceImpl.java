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
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImGroupMapper;
import com.echoim.server.mapper.ImGroupMemberMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.file.FileService;
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

@Service
public class ImGroupChatServiceImpl implements ImGroupChatService {

    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int CONVERSATION_STATUS_NORMAL = 1;
    private static final int GROUP_STATUS_NORMAL = 1;
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final int MESSAGE_TYPE_IMAGE = 3;
    private static final int MESSAGE_TYPE_FILE = 5;
    private static final int FILE_BIZ_TYPE_IMAGE = 2;
    private static final int FILE_BIZ_TYPE_FILE = 4;
    private static final int MESSAGE_STATUS_SENT = 1;
    private static final String ACK_TYPE_SEND = "SEND";
    private static final String CHANGE_TYPE_MESSAGE_NEW = "MESSAGE_NEW";

    private final ImConversationMapper imConversationMapper;
    private final ImConversationUserMapper imConversationUserMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImGroupMapper imGroupMapper;
    private final ImGroupMemberMapper imGroupMemberMapper;
    private final ImWsPushService imWsPushService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public ImGroupChatServiceImpl(ImConversationMapper imConversationMapper,
                                  ImConversationUserMapper imConversationUserMapper,
                                  ImMessageMapper imMessageMapper,
                                  ImGroupMapper imGroupMapper,
                                  ImGroupMemberMapper imGroupMemberMapper,
                                  ImWsPushService imWsPushService,
                                  FileService fileService,
                                  ObjectMapper objectMapper) {
        this.imConversationMapper = imConversationMapper;
        this.imConversationUserMapper = imConversationUserMapper;
        this.imMessageMapper = imMessageMapper;
        this.imGroupMapper = imGroupMapper;
        this.imGroupMemberMapper = imGroupMemberMapper;
        this.imWsPushService = imWsPushService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> sendGroup(LoginUser loginUser, WsMessage message) {
        WsGroupChatData data = objectMapper.convertValue(message.getData(), WsGroupChatData.class);
        validateChatRequest(message, data);

        ImMessageEntity duplicate = imMessageMapper.selectByFromUserIdAndClientMsgId(loginUser.getUserId(), message.getClientMsgId());
        if (duplicate != null) {
            WsMessageItem duplicateItem = toWsMessageItem(duplicate);
            fileService.enrichWsMessage(loginUser.getUserId(), duplicateItem);
            return sendAckData(duplicateItem, true);
        }

        ImConversationEntity conversation = imConversationMapper.selectByIdForUpdate(data.getConversationId());
        validateGroupConversation(conversation, data.getGroupId());
        validateGroup(data.getGroupId());
        requireActiveMember(data.getGroupId(), loginUser.getUserId());
        ImFileEntity messageFile = validateAndLoadMessageFile(loginUser.getUserId(), data);

        ImMessageEntity entity = buildMessage(loginUser.getUserId(), data, message.getClientMsgId());
        entity.setSeqNo(nextSeqNo(conversation.getId()));
        try {
            imMessageMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            ImMessageEntity existing = imMessageMapper.selectByFromUserIdAndClientMsgId(loginUser.getUserId(), message.getClientMsgId());
            if (existing != null) {
                WsMessageItem existingItem = toWsMessageItem(existing);
                fileService.enrichWsMessage(loginUser.getUserId(), existingItem);
                return sendAckData(existingItem, true);
            }
            throw ex;
        }

        imConversationUserMapper.resetDeleted(conversation.getId(), loginUser.getUserId());
        List<Long> recipientUserIds = imGroupMemberMapper.selectActiveUserIdsByGroupId(data.getGroupId())
                .stream()
                .filter(userId -> !userId.equals(loginUser.getUserId()))
                .toList();
        if (!recipientUserIds.isEmpty()) {
            imConversationUserMapper.incrementUnreadBatch(conversation.getId(), recipientUserIds);
        }
        imConversationMapper.updateLastMessage(conversation.getId(), entity.getId(), preview(entity, messageFile));

        WsMessageItem senderItem = toWsMessageItem(entity);
        fileService.enrichWsMessage(loginUser.getUserId(), senderItem);
        pushConversationChange(loginUser.getUserId(), conversation.getId(), senderItem);
        for (Long recipientUserId : recipientUserIds) {
            WsMessageItem recipientItem = toWsMessageItem(entity);
            fileService.enrichWsMessage(recipientUserId, recipientItem);
            pushGroupMessage(recipientUserId, message, recipientItem);
            pushConversationChange(recipientUserId, conversation.getId(), recipientItem);
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
        if ((msgType == MESSAGE_TYPE_IMAGE || msgType == MESSAGE_TYPE_FILE) && data.getFileId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件消息必须传 fileId");
        }
    }

    private void validateGroupConversation(ImConversationEntity conversation, Long groupId) {
        if (conversation == null || !Integer.valueOf(CONVERSATION_STATUS_NORMAL).equals(conversation.getStatus())
                || !Integer.valueOf(CONVERSATION_TYPE_GROUP).equals(conversation.getConversationType())
                || !groupId.equals(conversation.getBizId())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群会话不存在");
        }
    }

    private void validateGroup(Long groupId) {
        ImGroupEntity group = imGroupMapper.selectById(groupId);
        if (group == null || !Integer.valueOf(GROUP_STATUS_NORMAL).equals(group.getStatus())) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "群组不存在");
        }
    }

    private void requireActiveMember(Long groupId, Long userId) {
        ImGroupMemberEntity member = imGroupMemberMapper.selectActiveByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new BizException(ErrorCode.BUSINESS_CONFLICT, "不是正常群成员，无法发送群消息");
        }
    }

    private ImMessageEntity buildMessage(Long fromUserId, WsGroupChatData data, String clientMsgId) {
        ImMessageEntity entity = new ImMessageEntity();
        entity.setConversationId(data.getConversationId());
        entity.setConversationType(CONVERSATION_TYPE_GROUP);
        entity.setClientMsgId(clientMsgId);
        entity.setFromUserId(fromUserId);
        entity.setGroupId(data.getGroupId());
        entity.setMsgType(toMessageType(data.getMsgType()));
        entity.setContent(data.getContent());
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
        if ("IMAGE".equalsIgnoreCase(msgType)) {
            return MESSAGE_TYPE_IMAGE;
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
        if (Integer.valueOf(MESSAGE_TYPE_IMAGE).equals(msgType)) {
            return "IMAGE";
        }
        if (Integer.valueOf(MESSAGE_TYPE_FILE).equals(msgType)) {
            return "FILE";
        }
        return "UNKNOWN";
    }

    private ImFileEntity validateAndLoadMessageFile(Long userId, WsGroupChatData data) {
        int msgType = toMessageType(data.getMsgType());
        if (msgType == MESSAGE_TYPE_IMAGE) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_IMAGE);
        }
        if (msgType == MESSAGE_TYPE_FILE) {
            return fileService.requireOwnedFile(userId, data.getFileId(), FILE_BIZ_TYPE_FILE);
        }
        return null;
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
        if (Integer.valueOf(MESSAGE_TYPE_IMAGE).equals(entity.getMsgType())) {
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

    private WsMessageItem toWsMessageItem(ImMessageEntity entity) {
        WsMessageItem item = new WsMessageItem();
        item.setMessageId(entity.getId());
        item.setConversationId(entity.getConversationId());
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

    private void pushConversationChange(Long userId, Long conversationId, Object message) {
        var conversation = imConversationMapper.selectConversationItemByUserId(conversationId, userId);
        if (conversation != null) {
            imWsPushService.pushConversationChange(userId, CHANGE_TYPE_MESSAGE_NEW, conversation, message);
        }
    }
}
