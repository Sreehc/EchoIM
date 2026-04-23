package com.echoim.server.dto.conversation;

import jakarta.validation.constraints.Min;

public class MessagePageQueryDto {

    @Min(value = 1, message = "pageNo 最小为 1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "pageSize 最小为 1")
    private Long pageSize = 20L;

    @Min(value = 1, message = "maxSeqNo 最小为 1")
    private Long maxSeqNo;

    public Long getPageNo() {
        return pageNo;
    }

    public void setPageNo(Long pageNo) {
        this.pageNo = pageNo;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getMaxSeqNo() {
        return maxSeqNo;
    }

    public void setMaxSeqNo(Long maxSeqNo) {
        this.maxSeqNo = maxSeqNo;
    }
}
