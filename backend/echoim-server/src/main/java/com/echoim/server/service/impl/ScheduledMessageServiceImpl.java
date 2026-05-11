package com.echoim.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echoim.server.common.constant.ErrorCode;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.dto.message.CreateScheduledMessageRequestDto;
import com.echoim.server.entity.ImConversationEntity;
import com.echoim.server.entity.ImConversationUserEntity;
import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.entity.ImScheduledMessageEntity;
import com.echoim.server.im.model.WsChatSingleData;
import com.echoim.server.im.model.WsGroupChatData;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.im.model.WsMessageType;
import com.echoim.server.im.service.ImGroupChatService;
import com.echoim.server.im.service.ImSingleChatService;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImConversationUserMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImScheduledMessageMapper;
import com.echoim.server.service.message.ScheduledMessageService;
import com.echoim.server.vo.message.ScheduledMessageItemVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduledMessageServiceImpl implements ScheduledMessageService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageServiceImpl.class);

    private final ImScheduledMessageMapper scheduledMessageMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationUserMapper conversationUserMapper;
    private final ImMessageMapper messageMapper;
    private final ImSingleChatService singleChatService;
    private final ImGroupChatService groupChatService;
    private final ObjectMapper objectMapper;

    public ScheduledMessageServiceImpl(ImScheduledMessageMapper scheduledMessageMapper,
                                       ImConversationMapper conversationMapper,
                                       ImConversationUserMapper conversationUserMapper,
                                       ImMessageMapper messageMapper,
                                       ImSingleChatService singleChatService,
                                       ImGroupChatService groupChatService,
                                       ObjectMapper objectMapper) {
        this.scheduledMessageMapper = scheduledMessageMapper;
        this.conversationMapper = conversationMapper;
        this.conversationUserMapper = conversationUserMapper;
        this.messageMapper = messageMapper;
        this.singleChatService = singleChatService;
        this.groupChatService = groupChatService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ScheduledMessageItemVo createScheduledMessage(Long userId, CreateScheduledMessageRequestDto request) {
        // Validate conversation exists and user is member
        ImConversationEntity conversation = conversationMapper.selectById(request.getConversationId());
        if (conversation == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "会话不存在");
        }

        ImConversationUserEntity member = conversationUserMapper.selectByConversationIdAndUserId(
                request.getConversationId(), userId);
        if (member == null) {
            throw new BizException(ErrorCode.CONVERSATION_NOT_FOUND, "您不在此会话中");
        }

        // Build extra JSON with mentions if provided
        String extraJson = null;
        if (request.getExtraJson() != null || (request.getMentions() != null && !request.getMentions().isEmpty())) {
            try {
                Map<String, Object> extra = new HashMap<>();
                if (request.getExtraJson() != null) {
                    if (request.getExtraJson() instanceof Map) {
                        extra.putAll((Map<String, Object>) request.getExtraJson());
                    }
                }
                if (request.getMentions() != null && !request.getMentions().isEmpty()) {
                    extra.put("mentions", request.getMentions());
                }
                if (!extra.isEmpty()) {
                    extraJson = objectMapper.writeValueAsString(extra);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize extraJson", e);
            }
        }

        // Create scheduled message entity
        ImScheduledMessageEntity entity = new ImScheduledMessageEntity();
        entity.setUserId(userId);
        entity.setConversationId(request.getConversationId());
        entity.setConversationType(conversation.getConversationType());
        entity.setMsgType(request.getMsgType());
        entity.setContent(request.getContent());
        entity.setExtraJson(extraJson);
        entity.setFileId(request.getFileId());
        entity.setScheduledAt(request.getScheduledAt());
        entity.setStatus(ImScheduledMessageEntity.STATUS_PENDING);

        // Set toUserId or groupId based on conversation type
        if (conversation.getConversationType() == 1) {
            // Single chat - find the other user
            List<ImConversationUserEntity> members = conversationUserMapper.selectByConversationId(
                    request.getConversationId());
            for (ImConversationUserEntity m : members) {
                if (!m.getUserId().equals(userId)) {
                    entity.setToUserId(m.getUserId());
                    break;
                }
            }
        } else if (conversation.getConversationType() == 2 || conversation.getConversationType() == 3) {
            entity.setGroupId(conversation.getBizId());
        }

        scheduledMessageMapper.insert(entity);
        return toVo(entity);
    }

    @Override
    public List<ScheduledMessageItemVo> listScheduledMessages(Long userId, Long conversationId) {
        LambdaQueryWrapper<ImScheduledMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImScheduledMessageEntity::getUserId, userId)
                .eq(ImScheduledMessageEntity::getConversationId, conversationId)
                .eq(ImScheduledMessageEntity::getStatus, ImScheduledMessageEntity.STATUS_PENDING)
                .orderByAsc(ImScheduledMessageEntity::getScheduledAt);

        return scheduledMessageMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelScheduledMessage(Long userId, Long scheduledMessageId) {
        ImScheduledMessageEntity entity = scheduledMessageMapper.selectById(scheduledMessageId);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "定时消息不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "无权操作此定时消息");
        }
        if (entity.getStatus() != ImScheduledMessageEntity.STATUS_PENDING) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该定时消息已不可取消");
        }

        entity.setStatus(ImScheduledMessageEntity.STATUS_CANCELLED);
        scheduledMessageMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void sendScheduledMessageImmediately(Long userId, Long scheduledMessageId) {
        ImScheduledMessageEntity entity = scheduledMessageMapper.selectById(scheduledMessageId);
        if (entity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "定时消息不存在");
        }
        if (!entity.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "无权操作此定时消息");
        }
        if (entity.getStatus() != ImScheduledMessageEntity.STATUS_PENDING) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该定时消息已不可发送");
        }

        executeScheduledMessage(entity);
    }

    @Override
    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    @Transactional
    public void executePendingMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<ImScheduledMessageEntity> pendingMessages = scheduledMessageMapper.selectPendingBeforeTime(now);

        for (ImScheduledMessageEntity message : pendingMessages) {
            try {
                executeScheduledMessage(message);
            } catch (Exception e) {
                log.error("Failed to execute scheduled message {}: {}", message.getId(), e.getMessage(), e);
                message.setStatus(ImScheduledMessageEntity.STATUS_FAILED);
                message.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 255)) : "Unknown error");
                scheduledMessageMapper.updateById(message);
            }
        }
    }

    private void executeScheduledMessage(ImScheduledMessageEntity scheduled) {
        // Create a WsMessage to pass to the chat services
        WsMessage wsMessage = new WsMessage();
        wsMessage.setClientMsgId("scheduled-" + scheduled.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));

        if (scheduled.getConversationType() == 1) {
            // Single chat
            WsChatSingleData data = new WsChatSingleData();
            data.setConversationId(scheduled.getConversationId());
            data.setToUserId(scheduled.getToUserId());
            data.setMsgType(toMsgTypeString(scheduled.getMsgType()));
            data.setContent(scheduled.getContent());
            data.setFileId(scheduled.getFileId());
            if (scheduled.getExtraJson() != null) {
                try {
                    data.setExtraJson(objectMapper.readValue(scheduled.getExtraJson(), Object.class));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse extraJson for scheduled message {}", scheduled.getId(), e);
                }
            }
            wsMessage.setData(data);

            // Create a mock LoginUser for the scheduled message
            com.echoim.server.common.auth.LoginUser loginUser = new com.echoim.server.common.auth.LoginUser();
            loginUser.setUserId(scheduled.getUserId());

            Map<String, Object> result = singleChatService.sendSingle(loginUser, wsMessage);
            // Extract message ID from result if available
            if (result != null && result.containsKey("messageId")) {
                scheduled.setSentMessageId(Long.valueOf(result.get("messageId").toString()));
            }
        } else if (scheduled.getConversationType() == 2 || scheduled.getConversationType() == 3) {
            // Group/channel chat
            WsGroupChatData data = new WsGroupChatData();
            data.setConversationId(scheduled.getConversationId());
            data.setGroupId(scheduled.getGroupId());
            data.setMsgType(toMsgTypeString(scheduled.getMsgType()));
            data.setContent(scheduled.getContent());
            data.setFileId(scheduled.getFileId());
            if (scheduled.getExtraJson() != null) {
                try {
                    data.setExtraJson(objectMapper.readValue(scheduled.getExtraJson(), Object.class));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse extraJson for scheduled message {}", scheduled.getId(), e);
                }
            }
            wsMessage.setData(data);

            com.echoim.server.common.auth.LoginUser loginUser = new com.echoim.server.common.auth.LoginUser();
            loginUser.setUserId(scheduled.getUserId());

            Map<String, Object> result = groupChatService.sendGroup(loginUser, wsMessage);
            if (result != null && result.containsKey("messageId")) {
                scheduled.setSentMessageId(Long.valueOf(result.get("messageId").toString()));
            }
        }

        scheduled.setStatus(ImScheduledMessageEntity.STATUS_SENT);
        scheduledMessageMapper.updateById(scheduled);
    }

    private String toMsgTypeString(Integer msgType) {
        if (msgType == null) return "TEXT";
        return switch (msgType) {
            case 1 -> "TEXT";
            case 2 -> "STICKER";
            case 3 -> "IMAGE";
            case 4 -> "GIF";
            case 5 -> "FILE";
            case 6 -> "SYSTEM";
            case 7 -> "VOICE";
            default -> "TEXT";
        };
    }

    private ScheduledMessageItemVo toVo(ImScheduledMessageEntity entity) {
        ScheduledMessageItemVo vo = new ScheduledMessageItemVo();
        vo.setId(entity.getId());
        vo.setConversationId(entity.getConversationId());
        vo.setConversationType(entity.getConversationType());
        vo.setToUserId(entity.getToUserId());
        vo.setGroupId(entity.getGroupId());
        vo.setMsgType(entity.getMsgType());
        vo.setContent(entity.getContent());
        vo.setFileId(entity.getFileId());
        vo.setScheduledAt(entity.getScheduledAt());
        vo.setStatus(entity.getStatus());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setSentMessageId(entity.getSentMessageId());
        vo.setCreatedAt(entity.getCreatedAt());

        // Parse extraJson
        if (entity.getExtraJson() != null) {
            try {
                vo.setExtraJson(objectMapper.readValue(entity.getExtraJson(), Object.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse extraJson for scheduled message {}", entity.getId(), e);
            }
        }

        return vo;
    }
}
