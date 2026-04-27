package com.lingring.domain.signaling.dao;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class LocalSessionRegistry {

    private final ConcurrentMap<Long, WebSocketSession> sessionsByUserId = new ConcurrentHashMap<>();

    public void register(final Long userId, final WebSocketSession session) {
        sessionsByUserId.put(userId, session);
    }

    public void unregister(final Long userId) {
        sessionsByUserId.remove(userId);
    }

    public Optional<WebSocketSession> find(final Long userId) {
        return Optional.ofNullable(sessionsByUserId.get(userId));
    }
}
