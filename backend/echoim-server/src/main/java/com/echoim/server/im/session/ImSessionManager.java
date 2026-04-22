package com.echoim.server.im.session;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImSessionManager {

    private final Map<Long, ImSessionContext> sessions = new ConcurrentHashMap<>();

    public void register(ImSessionContext context) {
        sessions.put(context.getUserId(), context);
    }

    public void remove(Long userId) {
        sessions.remove(userId);
    }

    public Optional<ImSessionContext> get(Long userId) {
        return Optional.ofNullable(sessions.get(userId));
    }

    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId);
    }

    public Collection<ImSessionContext> allSessions() {
        return sessions.values();
    }
}
