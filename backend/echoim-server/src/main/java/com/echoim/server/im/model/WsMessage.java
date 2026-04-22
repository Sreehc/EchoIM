package com.echoim.server.im.model;

public class WsMessage {

    private WsMessageType type;
    private String traceId;
    private String clientMsgId;
    private Long timestamp;
    private Object data;

    public WsMessageType getType() {
        return type;
    }

    public void setType(WsMessageType type) {
        this.type = type;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
