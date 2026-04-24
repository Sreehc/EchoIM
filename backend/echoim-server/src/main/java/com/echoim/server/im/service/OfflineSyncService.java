package com.echoim.server.im.service;

import com.echoim.server.dto.offline.OfflineSyncPointDto;
import com.echoim.server.dto.offline.OfflineSyncRequestDto;
import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.service.file.FileService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.offline.OfflineSyncConversationVo;
import com.echoim.server.vo.offline.OfflineSyncResponseVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OfflineSyncService {

    private static final int DEFAULT_PER_CONVERSATION_LIMIT = 50;
    private static final int DEFAULT_TOTAL_LIMIT = 500;

    private final ImConversationMapper imConversationMapper;
    private final ImMessageMapper imMessageMapper;
    private final FileService fileService;

    public OfflineSyncService(ImConversationMapper imConversationMapper,
                              ImMessageMapper imMessageMapper,
                              FileService fileService) {
        this.imConversationMapper = imConversationMapper;
        this.imMessageMapper = imMessageMapper;
        this.fileService = fileService;
    }

    public OfflineSyncResponseVo syncMessages(Long userId, OfflineSyncRequestDto requestDto) {
        long defaultLastSyncSeq = normalizeSeq(requestDto.getDefaultLastSyncSeq());
        int perConversationLimit = normalizeLimit(requestDto.getPerConversationLimit(), DEFAULT_PER_CONVERSATION_LIMIT);
        int totalLimit = normalizeLimit(requestDto.getTotalLimit(), DEFAULT_TOTAL_LIMIT);
        Map<Long, OfflineSyncPointDto> syncPointMap = normalizeSyncPoints(requestDto.getSyncPoints());

        List<ConversationItemVo> conversations = imConversationMapper.selectAllConversationsByUserId(userId);
        List<OfflineSyncConversationVo> result = new ArrayList<>();
        int remaining = totalLimit;
        boolean hasMore = false;

        for (ConversationItemVo conversation : conversations) {
            long lastSyncSeq = syncPointMap.containsKey(conversation.getConversationId())
                    ? normalizeSeq(syncPointMap.get(conversation.getConversationId()).getLastSyncSeq())
                    : defaultLastSyncSeq;
            long total = imMessageMapper.countMessageAfterSeqByConversationIdAndUserId(conversation.getConversationId(), userId, lastSyncSeq);
            if (total == 0) {
                continue;
            }
            if (remaining <= 0) {
                hasMore = true;
                continue;
            }

            int currentLimit = Math.min(perConversationLimit, remaining);
            List<MessageItemVo> messages = imMessageMapper.selectMessageAfterSeqByConversationIdAndUserId(
                    conversation.getConversationId(), userId, lastSyncSeq, currentLimit);
            if (messages.isEmpty()) {
                continue;
            }
            fileService.enrichMessages(userId, messages);

            OfflineSyncConversationVo item = new OfflineSyncConversationVo();
            item.setConversation(conversation);
            item.setMessages(messages);
            item.setFromSeq(messages.get(0).getSeqNo());
            item.setToSeq(messages.get(messages.size() - 1).getSeqNo());
            item.setHasMore(total > messages.size());
            result.add(item);

            remaining -= messages.size();
            if (Boolean.TRUE.equals(item.getHasMore())) {
                hasMore = true;
            }
        }

        return new OfflineSyncResponseVo(result, hasMore);
    }

    private Map<Long, OfflineSyncPointDto> normalizeSyncPoints(List<OfflineSyncPointDto> syncPoints) {
        if (syncPoints == null || syncPoints.isEmpty()) {
            return Map.of();
        }
        return syncPoints.stream()
                .filter(point -> point.getConversationId() != null)
                .collect(Collectors.toMap(OfflineSyncPointDto::getConversationId, Function.identity(), (left, right) -> right));
    }

    private long normalizeSeq(Long seq) {
        return seq == null || seq < 0 ? 0L : seq;
    }

    private int normalizeLimit(Integer limit, int defaultValue) {
        return limit == null || limit < 1 ? defaultValue : limit;
    }
}
