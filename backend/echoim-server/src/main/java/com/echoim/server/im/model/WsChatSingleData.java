package com.echoim.server.im.model;

public class WsChatSingleData {

    private Long conversationId;
    private Long toUserId;
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

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
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
