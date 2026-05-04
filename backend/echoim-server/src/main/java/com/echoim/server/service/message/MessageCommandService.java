package com.echoim.server.service.message;

import com.echoim.server.dto.message.EditMessageRequestDto;
import com.echoim.server.dto.message.ForwardMessageRequestDto;
import com.echoim.server.vo.conversation.MessageItemVo;

import java.util.List;
import java.util.Map;

public interface MessageCommandService {

    Map<String, Object> recall(Long userId, Long messageId);

    Map<String, Object> edit(Long userId, Long messageId, EditMessageRequestDto requestDto);

    Map<String, Object> forward(Long userId, ForwardMessageRequestDto requestDto);

    MessageItemVo toggleReaction(Long userId, Long messageId, String emoji);

    MessageItemVo pinMessage(Long userId, Long messageId);

    MessageItemVo unpinMessage(Long userId, Long messageId);

    List<MessageItemVo> listPinnedMessages(Long userId, Long conversationId);
}
