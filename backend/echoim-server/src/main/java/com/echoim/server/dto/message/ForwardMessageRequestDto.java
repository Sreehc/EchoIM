package com.echoim.server.dto.message;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ForwardMessageRequestDto {

    @NotEmpty(message = "源消息不能为空")
    private List<Long> messageIds;

    @NotEmpty(message = "目标会话不能为空")
    private List<Long> targetConversationIds;

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }

    public List<Long> getTargetConversationIds() {
        return targetConversationIds;
    }

    public void setTargetConversationIds(List<Long> targetConversationIds) {
        this.targetConversationIds = targetConversationIds;
    }
}
