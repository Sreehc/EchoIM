package com.echoim.server.dto.offline;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OfflineSyncPointDto {

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @Min(value = 0, message = "lastSyncSeq 最小为 0")
    private Long lastSyncSeq = 0L;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getLastSyncSeq() {
        return lastSyncSeq;
    }

    public void setLastSyncSeq(Long lastSyncSeq) {
        this.lastSyncSeq = lastSyncSeq;
    }
}
