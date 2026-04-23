package com.echoim.server.im.model;

public class WsAckData {

    private String ackType;
    private Long conversationId;
    private Long messageId;
    private Long seqNo;

    public String getAckType() {
        return ackType;
    }

    public void setAckType(String ackType) {
        this.ackType = ackType;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Long seqNo) {
        this.seqNo = seqNo;
    }
}
