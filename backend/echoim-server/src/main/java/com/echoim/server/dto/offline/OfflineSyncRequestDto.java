package com.echoim.server.dto.offline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.ArrayList;
import java.util.List;

public class OfflineSyncRequestDto {

    @Min(value = 0, message = "defaultLastSyncSeq 最小为 0")
    private Long defaultLastSyncSeq = 0L;

    @Valid
    private List<OfflineSyncPointDto> syncPoints = new ArrayList<>();

    @Min(value = 1, message = "perConversationLimit 最小为 1")
    private Integer perConversationLimit = 50;

    @Min(value = 1, message = "totalLimit 最小为 1")
    private Integer totalLimit = 500;

    public Long getDefaultLastSyncSeq() {
        return defaultLastSyncSeq;
    }

    public void setDefaultLastSyncSeq(Long defaultLastSyncSeq) {
        this.defaultLastSyncSeq = defaultLastSyncSeq;
    }

    public List<OfflineSyncPointDto> getSyncPoints() {
        return syncPoints;
    }

    public void setSyncPoints(List<OfflineSyncPointDto> syncPoints) {
        this.syncPoints = syncPoints;
    }

    public Integer getPerConversationLimit() {
        return perConversationLimit;
    }

    public void setPerConversationLimit(Integer perConversationLimit) {
        this.perConversationLimit = perConversationLimit;
    }

    public Integer getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(Integer totalLimit) {
        this.totalLimit = totalLimit;
    }
}
