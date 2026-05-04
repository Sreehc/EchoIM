package com.echoim.server.service.impl;

import com.echoim.server.mapper.ImConversationMapper;
import com.echoim.server.mapper.ImMessageMapper;
import com.echoim.server.mapper.ImUserMapper;
import com.echoim.server.service.search.GlobalSearchService;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.search.GlobalSearchMessageItemVo;
import com.echoim.server.vo.search.GlobalSearchResponseVo;
import com.echoim.server.vo.user.UserSearchItemVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final ImConversationMapper imConversationMapper;
    private final ImUserMapper imUserMapper;
    private final ImMessageMapper imMessageMapper;

    public GlobalSearchServiceImpl(ImConversationMapper imConversationMapper,
                                   ImUserMapper imUserMapper,
                                   ImMessageMapper imMessageMapper) {
        this.imConversationMapper = imConversationMapper;
        this.imUserMapper = imUserMapper;
        this.imMessageMapper = imMessageMapper;
    }

    @Override
    public GlobalSearchResponseVo search(Long currentUserId, String keyword, Integer conversationLimit, Integer userLimit, Integer messageLimit) {
        return search(currentUserId, keyword, conversationLimit, userLimit, messageLimit, null, null, null);
    }

    @Override
    public GlobalSearchResponseVo search(Long currentUserId, String keyword, Integer conversationLimit, Integer userLimit, Integer messageLimit,
                                         String msgType, LocalDateTime dateFrom, LocalDateTime dateTo) {
        GlobalSearchResponseVo responseVo = new GlobalSearchResponseVo();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(normalizedKeyword)) {
            responseVo.setConversations(List.of());
            responseVo.setUsers(List.of());
            responseVo.setMessages(List.of());
            return responseVo;
        }

        int nextConversationLimit = normalizeLimit(conversationLimit, 8);
        int nextUserLimit = normalizeLimit(userLimit, 8);
        int nextMessageLimit = normalizeLimit(messageLimit, 12);
        String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);

        List<ConversationItemVo> conversations = imConversationMapper.selectAllConversationsByUserId(currentUserId).stream()
                .filter(item -> containsIgnoreCase(item.getConversationName(), lowerKeyword)
                        || containsIgnoreCase(item.getLastMessagePreview(), lowerKeyword))
                .limit(nextConversationLimit)
                .toList();
        List<UserSearchItemVo> users = imUserMapper.selectSearchPage(currentUserId, normalizedKeyword, 0, nextUserLimit);
        List<GlobalSearchMessageItemVo> messages = imMessageMapper.selectGlobalSearchMessages(currentUserId, normalizedKeyword, nextMessageLimit,
                normalizeMsgType(msgType), dateFrom, dateTo);

        responseVo.setConversations(conversations);
        responseVo.setUsers(users);
        responseVo.setMessages(messages);
        return responseVo;
    }

    private int normalizeLimit(Integer value, int fallback) {
        if (value == null || value < 1) {
            return fallback;
        }
        return Math.min(value, 20);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Integer normalizeMsgType(String msgType) {
        if (msgType == null || msgType.isBlank()) {
            return null;
        }
        return switch (msgType.toUpperCase(Locale.ROOT)) {
            case "IMAGE" -> 3;
            case "FILE" -> 5;
            case "VOICE" -> 7;
            case "GIF" -> 4;
            case "STICKER" -> 2;
            default -> null;
        };
    }
}
