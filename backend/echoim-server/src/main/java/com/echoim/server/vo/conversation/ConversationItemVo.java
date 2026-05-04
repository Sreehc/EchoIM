package com.echoim.server.vo.conversation;

import java.time.LocalDateTime;

public class ConversationItemVo {

    private Long conversationId;
    private String conversationNo;
    private Integer conversationType;
    private String conversationName;
    private String avatarUrl;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Integer isTop;
    private Integer isMute;
    private Long peerUserId;
    private Long groupId;
    private Long latestSeq;
    private Boolean canSend;
    private Integer groupStatus;
    private Integer myRole;
    private Boolean archived;
    private Boolean manualUnread;
    private String specialType;
    private String draftContent;
    private java.util.List<String> folderHints;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationNo() {
        return conversationNo;
    }

    public void setConversationNo(String conversationNo) {
        this.conversationNo = conversationNo;
    }

    public Integer getConversationType() {
        return conversationType;
    }

    public void setConversationType(Integer conversationType) {
        this.conversationType = conversationType;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Integer getIsTop() {
        return isTop;
    }

    public void setIsTop(Integer isTop) {
        this.isTop = isTop;
    }

    public Integer getIsMute() {
        return isMute;
    }

    public void setIsMute(Integer isMute) {
        this.isMute = isMute;
    }

    public Long getPeerUserId() {
        return peerUserId;
    }

    public void setPeerUserId(Long peerUserId) {
        this.peerUserId = peerUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getLatestSeq() {
        return latestSeq;
    }

    public void setLatestSeq(Long latestSeq) {
        this.latestSeq = latestSeq;
    }

    public Boolean getCanSend() {
        return canSend;
    }

    public void setCanSend(Boolean canSend) {
        this.canSend = canSend;
    }

    public Integer getGroupStatus() {
        return groupStatus;
    }

    public void setGroupStatus(Integer groupStatus) {
        this.groupStatus = groupStatus;
    }

    public Integer getMyRole() {
        return myRole;
    }

    public void setMyRole(Integer myRole) {
        this.myRole = myRole;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getManualUnread() {
        return manualUnread;
    }

    public void setManualUnread(Boolean manualUnread) {
        this.manualUnread = manualUnread;
    }

    public String getDraftContent() {
        return draftContent;
    }

    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    public String getSpecialType() {
        return specialType;
    }

    public void setSpecialType(String specialType) {
        this.specialType = specialType;
    }

    public java.util.List<String> getFolderHints() {
        return folderHints;
    }

    public void setFolderHints(java.util.List<String> folderHints) {
        this.folderHints = folderHints;
    }
}
