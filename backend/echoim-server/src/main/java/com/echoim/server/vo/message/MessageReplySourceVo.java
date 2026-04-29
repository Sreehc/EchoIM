package com.echoim.server.vo.message;

public class MessageReplySourceVo {

    private Long sourceMessageId;
    private Long sourceConversationId;
    private Long sourceSenderId;
    private String sourceMsgType;
    private String sourcePreview;

    public Long getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(Long sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public Long getSourceConversationId() {
        return sourceConversationId;
    }

    public void setSourceConversationId(Long sourceConversationId) {
        this.sourceConversationId = sourceConversationId;
    }

    public Long getSourceSenderId() {
        return sourceSenderId;
    }

    public void setSourceSenderId(Long sourceSenderId) {
        this.sourceSenderId = sourceSenderId;
    }

    public String getSourceMsgType() {
        return sourceMsgType;
    }

    public void setSourceMsgType(String sourceMsgType) {
        this.sourceMsgType = sourceMsgType;
    }

    public String getSourcePreview() {
        return sourcePreview;
    }

    public void setSourcePreview(String sourcePreview) {
        this.sourcePreview = sourcePreview;
    }
}
