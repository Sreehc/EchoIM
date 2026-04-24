package com.echoim.server.service.conversation;

import com.echoim.server.common.PageResponse;
import com.echoim.server.dto.conversation.ConversationPageQueryDto;
import com.echoim.server.dto.conversation.MessagePageQueryDto;
import com.echoim.server.vo.conversation.ConversationItemVo;
import com.echoim.server.vo.conversation.MessageItemVo;

public interface ConversationService {

    PageResponse<ConversationItemVo> pageCurrentUserConversations(Long userId, ConversationPageQueryDto queryDto);

    PageResponse<MessageItemVo> pageConversationMessages(Long userId, Long conversationId, MessagePageQueryDto queryDto);

    void readConversation(Long userId, Long conversationId, Long lastReadSeq);

    void updateTop(Long userId, Long conversationId, Integer isTop);

    void updateMute(Long userId, Long conversationId, Integer isMute);

    void deleteConversation(Long userId, Long conversationId);
}
