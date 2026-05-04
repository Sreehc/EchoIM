package com.echoim.server.service.search;

import com.echoim.server.vo.search.GlobalSearchResponseVo;

import java.time.LocalDateTime;

public interface GlobalSearchService {

    GlobalSearchResponseVo search(Long currentUserId, String keyword, Integer conversationLimit, Integer userLimit, Integer messageLimit);

    GlobalSearchResponseVo search(Long currentUserId, String keyword, Integer conversationLimit, Integer userLimit, Integer messageLimit,
                                  String msgType, LocalDateTime dateFrom, LocalDateTime dateTo);
}
