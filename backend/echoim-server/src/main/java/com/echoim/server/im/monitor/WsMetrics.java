package com.echoim.server.im.monitor;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks WebSocket message throughput and connection lifecycle counters.
 * All counters are cumulative since application start.
 */
@Component
public class WsMetrics {

    private final AtomicLong messagesReceived = new AtomicLong();
    private final AtomicLong messagesSent = new AtomicLong();
    private final AtomicLong connectionsOpened = new AtomicLong();
    private final AtomicLong connectionsClosed = new AtomicLong();
    private final AtomicLong authFailures = new AtomicLong();

    public void recordMessageReceived() {
        messagesReceived.incrementAndGet();
    }

    public void recordMessageSent() {
        messagesSent.incrementAndGet();
    }

    public void recordConnectionOpened() {
        connectionsOpened.incrementAndGet();
    }

    public void recordConnectionClosed() {
        connectionsClosed.incrementAndGet();
    }

    public void recordAuthFailure() {
        authFailures.incrementAndGet();
    }

    public long getMessagesReceived() {
        return messagesReceived.get();
    }

    public long getMessagesSent() {
        return messagesSent.get();
    }

    public long getConnectionsOpened() {
        return connectionsOpened.get();
    }

    public long getConnectionsClosed() {
        return connectionsClosed.get();
    }

    public long getAuthFailures() {
        return authFailures.get();
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                "messagesReceived", messagesReceived.get(),
                "messagesSent", messagesSent.get(),
                "connectionsOpened", connectionsOpened.get(),
                "connectionsClosed", connectionsClosed.get(),
                "authFailures", authFailures.get()
        );
    }
}
