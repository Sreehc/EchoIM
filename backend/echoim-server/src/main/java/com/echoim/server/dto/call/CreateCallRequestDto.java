package com.echoim.server.dto.call;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCallRequestDto {

    @NotNull(message = "会话不能为空")
    private Long conversationId;

    @NotBlank(message = "通话类型不能为空")
    private String callType;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }
}
