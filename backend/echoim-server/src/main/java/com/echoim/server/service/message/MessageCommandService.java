package com.echoim.server.service.message;

import com.echoim.server.dto.message.EditMessageRequestDto;
import com.echoim.server.dto.message.ForwardMessageRequestDto;

import java.util.Map;

public interface MessageCommandService {

    Map<String, Object> recall(Long userId, Long messageId);

    Map<String, Object> edit(Long userId, Long messageId, EditMessageRequestDto requestDto);

    Map<String, Object> forward(Long userId, ForwardMessageRequestDto requestDto);
}
