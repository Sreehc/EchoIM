package com.echoim.server.service.message;

import com.echoim.server.dto.message.CreateScheduledMessageRequestDto;
import com.echoim.server.vo.message.ScheduledMessageItemVo;

import java.util.List;

public interface ScheduledMessageService {

    ScheduledMessageItemVo createScheduledMessage(Long userId, CreateScheduledMessageRequestDto request);

    List<ScheduledMessageItemVo> listScheduledMessages(Long userId, Long conversationId);

    void cancelScheduledMessage(Long userId, Long scheduledMessageId);

    void sendScheduledMessageImmediately(Long userId, Long scheduledMessageId);

    void executePendingMessages();
}
