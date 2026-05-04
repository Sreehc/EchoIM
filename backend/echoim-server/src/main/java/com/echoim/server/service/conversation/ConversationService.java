package com.echoim.server.service.conversation;

import com.echoim.server.common.PageResponse;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.vo.conversation.ConversationFileVo;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;

public interface ConversationService {

    PageResponse<ConversationItemVo> pageCurrentUserConversations(Long userId, ConversationPageQueryDto queryDto);

    PageResponse<MessageItemVo> pageConversationMessages(Long userId, Long conversationId, MessagePageQueryDto queryDto);

    PageResponse<ConversationFileVo> pageConversationFiles(Long userId, Long conversationId, long pageNo, long pageSize);

    void readConversation(Long userId, Long conversationId, Long lastReadSeq);

    ConversationItemVo createSingleConversation(Long userId, Long targetUserId);

    ConversationItemVo createSavedConversation(Long userId);

    void updateTop(Long userId, Long conversationId, Integer isTop);

    void updateMute(Long userId, Long conversationId, Integer isMute);

    void updateArchive(Long userId, Long conversationId, boolean archived);

    void markConversationUnread(Long userId, Long conversationId, boolean unread);

    void deleteConversation(Long userId, Long conversationId);

    void saveDraft(Long userId, Long conversationId, String draftContent);

    String loadDraft(Long userId, Long conversationId);
}
