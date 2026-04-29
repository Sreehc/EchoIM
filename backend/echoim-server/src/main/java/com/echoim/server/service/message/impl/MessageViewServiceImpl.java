package com.echoim.server.service.message.impl;

import com.echoim.server.entity.ImMessageEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.mapper.ImMessageReactionMapper;
import com.echoim.server.mapper.ImMessageReceiptMapper;
import com.echoim.server.service.message.MessageViewService;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.message.MessageForwardSourceVo;
import com.echoim.server.vo.message.MessageReactionStatVo;
import com.echoim.server.vo.message.MessageReplySourceVo;
import com.echoim.server.vo.message.MessageReceiptStatVo;
import com.echoim.server.vo.message.StickerPayloadVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageViewServiceImpl implements MessageViewService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_TYPE_CHANNEL = 3;
    private static final int MESSAGE_STATUS_RECALLED = 3;

    private final ImMessageReceiptMapper imMessageReceiptMapper;
    private final ImMessageReactionMapper imMessageReactionMapper;
    private final ObjectMapper objectMapper;

    public MessageViewServiceImpl(ImMessageReceiptMapper imMessageReceiptMapper,
                                  ImMessageReactionMapper imMessageReactionMapper,
                                  ObjectMapper objectMapper) {
        this.imMessageReceiptMapper = imMessageReceiptMapper;
        this.imMessageReactionMapper = imMessageReactionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enrichMessages(Long viewerUserId, List<MessageItemVo> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        Map<Long, MessageReceiptStatVo> receiptMap = loadReceiptMap(messages, viewerUserId);
        Map<Long, List<MessageReactionStatVo>> reactionMap = loadReactionMap(messages.stream()
                .map(MessageItemVo::getMessageId)
                .filter(Objects::nonNull)
                .toList(), viewerUserId);
        for (MessageItemVo message : messages) {
            applyExtra(message, viewerUserId);
            message.setReactions(reactionMap.getOrDefault(message.getMessageId(), List.of()));
            MessageReceiptStatVo stat = receiptMap.get(message.getMessageId());
            if (stat != null) {
                message.setDelivered(stat.getDeliveredAt() != null);
                message.setDeliveredAt(stat.getDeliveredAt());
                message.setRead(stat.getReadAt() != null);
                message.setReadAt(stat.getReadAt());
                message.setViewCount(stat.getViewCount());
            }
        }
    }

    @Override
    public void enrichWsMessage(Long viewerUserId, WsMessageItem item, ImMessageEntity entity) {
        if (item == null || entity == null) {
            return;
        }
        Map<String, Object> extraMap = parseExtra(entity.getExtraJson());
        applyExtra(item, viewerUserId, extraMap);
        item.setReactions(loadReactionMap(List.of(entity.getId()), viewerUserId).getOrDefault(entity.getId(), List.of()));
        if (Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(entity.getConversationType())
                && Objects.equals(entity.getFromUserId(), viewerUserId)) {
            List<MessageReceiptStatVo> stats = imMessageReceiptMapper.selectReceiptStatsByMessageIds(List.of(entity.getId()));
            if (!stats.isEmpty()) {
                MessageReceiptStatVo stat = stats.get(0);
                item.setDelivered(stat.getDeliveredAt() != null);
                item.setDeliveredAt(stat.getDeliveredAt());
                item.setRead(stat.getReadAt() != null);
                item.setReadAt(stat.getReadAt());
            }
        } else if (Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(entity.getConversationType())
                && Objects.equals(entity.getFromUserId(), viewerUserId)) {
            List<MessageReceiptStatVo> stats = imMessageReceiptMapper.selectChannelViewStatsByMessageIds(List.of(entity.getId()));
            if (!stats.isEmpty()) {
                item.setViewCount(stats.get(0).getViewCount());
            }
        }
    }

    private Map<Long, MessageReceiptStatVo> loadReceiptMap(List<MessageItemVo> messages, Long viewerUserId) {
        List<Long> singleSenderMessageIds = messages.stream()
                .filter(message -> Integer.valueOf(CONVERSATION_TYPE_SINGLE).equals(resolveConversationType(message)))
                .filter(message -> Objects.equals(message.getFromUserId(), viewerUserId))
                .map(MessageItemVo::getMessageId)
                .toList();
        Map<Long, MessageReceiptStatVo> receiptMap = new HashMap<>();
        if (!singleSenderMessageIds.isEmpty()) {
            for (MessageReceiptStatVo stat : imMessageReceiptMapper.selectReceiptStatsByMessageIds(singleSenderMessageIds)) {
                receiptMap.put(stat.getMessageId(), stat);
            }
        }

        List<Long> channelSenderMessageIds = messages.stream()
                .filter(message -> Integer.valueOf(CONVERSATION_TYPE_CHANNEL).equals(resolveConversationType(message)))
                .filter(message -> Objects.equals(message.getFromUserId(), viewerUserId))
                .map(MessageItemVo::getMessageId)
                .toList();
        if (!channelSenderMessageIds.isEmpty()) {
            for (MessageReceiptStatVo stat : imMessageReceiptMapper.selectChannelViewStatsByMessageIds(channelSenderMessageIds)) {
                receiptMap.compute(stat.getMessageId(), (messageId, existing) -> {
                    if (existing == null) {
                        existing = new MessageReceiptStatVo();
                        existing.setMessageId(messageId);
                    }
                    existing.setViewCount(stat.getViewCount());
                    return existing;
                });
            }
        }
        return receiptMap;
    }

    private Integer resolveConversationType(MessageItemVo message) {
        return message.getConversationType() == null
                ? (message.getGroupId() == null ? CONVERSATION_TYPE_SINGLE : 2)
                : message.getConversationType();
    }

    private void applyExtra(MessageItemVo item, Long viewerUserId) {
        Map<String, Object> extraMap = parseExtra(item.getExtraJsonRaw());
        applyExtra(item, viewerUserId, extraMap);
    }

    private void applyExtra(MessageItemVo item, Long viewerUserId, Map<String, Object> extraMap) {
        boolean recalled = Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(item.getSendStatus())
                || Boolean.TRUE.equals(extraMap.get("recalled"));
        item.setRecalled(recalled);
        item.setRecalledAt(readDateTime(extraMap.get("recalledAt")));
        item.setEdited(Boolean.TRUE.equals(extraMap.get("edited")));
        item.setEditedAt(readDateTime(extraMap.get("editedAt")));
        item.setForwardSource(readForwardSource(extraMap.get("forwardSource")));
        item.setReplySource(readReplySource(extraMap.get("replySource")));
        item.setSticker(readSticker(extraMap.get("sticker")));
        if (item.getViewCount() == null) {
            item.setViewCount(0);
        }
        if (recalled) {
            item.setContent(resolveRecallText(item.getFromUserId(), viewerUserId, item.getGroupId()));
            item.setFileId(null);
            item.setFile(null);
            item.setMsgType("SYSTEM");
        }
    }

    private void applyExtra(WsMessageItem item, Long viewerUserId, Map<String, Object> extraMap) {
        boolean recalled = Integer.valueOf(MESSAGE_STATUS_RECALLED).equals(item.getSendStatus())
                || Boolean.TRUE.equals(extraMap.get("recalled"));
        item.setRecalled(recalled);
        item.setRecalledAt(readDateTime(extraMap.get("recalledAt")));
        item.setEdited(Boolean.TRUE.equals(extraMap.get("edited")));
        item.setEditedAt(readDateTime(extraMap.get("editedAt")));
        item.setForwardSource(readForwardSource(extraMap.get("forwardSource")));
        item.setReplySource(readReplySource(extraMap.get("replySource")));
        item.setSticker(readSticker(extraMap.get("sticker")));
        if (item.getViewCount() == null) {
            item.setViewCount(0);
        }
        if (recalled) {
            item.setContent(resolveRecallText(item.getFromUserId(), viewerUserId, item.getGroupId()));
            item.setFileId(null);
            item.setFile(null);
            item.setMsgType("SYSTEM");
        }
    }

    private String resolveRecallText(Long fromUserId, Long viewerUserId, Long groupId) {
        if (Objects.equals(fromUserId, viewerUserId)) {
            return "你撤回了一条消息";
        }
        return groupId == null ? "对方撤回了一条消息" : "某成员撤回了一条消息";
    }

    private LocalDateTime readDateTime(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, LocalDateTime.class);
    }

    private MessageForwardSourceVo readForwardSource(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, MessageForwardSourceVo.class);
    }

    private MessageReplySourceVo readReplySource(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, MessageReplySourceVo.class);
    }

    private StickerPayloadVo readSticker(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, StickerPayloadVo.class);
    }

    private Map<Long, List<MessageReactionStatVo>> loadReactionMap(List<Long> messageIds, Long viewerUserId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MessageReactionStatVo>> reactionMap = new HashMap<>();
        for (MessageReactionStatVo stat : imMessageReactionMapper.selectReactionStatsByMessageIds(messageIds, viewerUserId)) {
            reactionMap.computeIfAbsent(stat.getMessageId(), ignored -> new ArrayList<>()).add(stat);
        }
        return reactionMap;
    }

    private Map<String, Object> parseExtra(String extraJson) {
        if (extraJson == null || extraJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(extraJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
