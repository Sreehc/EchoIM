package com.echoim.server.dto.conversation;

import jakarta.validation.constraints.Min;

public class ConversationPageQueryDto {

    @Min(value = 1, message = "pageNo 最小为 1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "pageSize 最小为 1")
    private Long pageSize = 20L;
    private Integer archived = 0;
    private String folder;

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

    public Integer getArchived() {
        return archived;
    }

    public void setArchived(Integer archived) {
        this.archived = archived;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
