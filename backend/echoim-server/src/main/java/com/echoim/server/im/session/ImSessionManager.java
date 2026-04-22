package com.echoim.server.im.session;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImSessionManager {

    private final Map<Long, ImSessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Channel> channels = new ConcurrentHashMap<>();

    public Channel register(ImSessionContext context, Channel channel) {
        sessions.put(context.getUserId(), context);
        return channels.put(context.getUserId(), channel);
    }

    public void remove(Long userId) {
        sessions.remove(userId);
        channels.remove(userId);
    }

    public boolean removeIfMatch(Long userId, Channel channel) {
        boolean removed = channels.remove(userId, channel);
        if (removed) {
            sessions.remove(userId);
        }
        return removed;
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

    public Optional<Channel> getChannel(Long userId) {
        return Optional.ofNullable(channels.get(userId));
    }
}
