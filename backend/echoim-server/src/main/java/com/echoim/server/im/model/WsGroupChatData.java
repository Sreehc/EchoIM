package com.echoim.server.im.model;

public class WsGroupChatData {

    private Long conversationId;
    private Long groupId;
    private String msgType;
    private String content;
    private Long fileId;
    private Object extraJson;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Object getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(Object extraJson) {
        this.extraJson = extraJson;
    }
}
