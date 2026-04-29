package com.echoim.server.service.search;

import com.echoim.server.vo.search.GlobalSearchResponseVo;

public interface GlobalSearchService {

    GlobalSearchResponseVo search(Long currentUserId, String keyword, Integer conversationLimit, Integer userLimit, Integer messageLimit);
}
