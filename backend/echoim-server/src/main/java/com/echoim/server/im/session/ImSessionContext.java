package com.echoim.server.im.session;

import java.time.LocalDateTime;

public class ImSessionContext {

    private final Long userId;
    private final String sessionId;
    private final LocalDateTime connectedAt;

    public ImSessionContext(Long userId, String sessionId, LocalDateTime connectedAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.connectedAt = connectedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
}
