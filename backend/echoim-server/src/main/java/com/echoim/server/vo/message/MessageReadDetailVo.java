package com.echoim.server.vo.message;

import java.util.List;

public class MessageReadDetailVo {

    private Long messageId;
    private Integer totalMembers;
    private Integer readCount;
    private Integer unreadCount;
    private List<MessageReadDetailItemVo> readList;
    private List<MessageReadDetailItemVo> unreadList;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Integer getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(Integer totalMembers) {
        this.totalMembers = totalMembers;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public List<MessageReadDetailItemVo> getReadList() {
        return readList;
    }

    public void setReadList(List<MessageReadDetailItemVo> readList) {
        this.readList = readList;
    }

    public List<MessageReadDetailItemVo> getUnreadList() {
        return unreadList;
    }

    public void setUnreadList(List<MessageReadDetailItemVo> unreadList) {
        this.unreadList = unreadList;
    }
}
