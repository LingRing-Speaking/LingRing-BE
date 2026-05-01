package com.lingring.domain.signaling.dao;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class LocalSessionRegistry {

    private final ConcurrentMap<Long, WebSocketSession> sessionsByUserId = new ConcurrentHashMap<>();

    public void register(final Long userId, final WebSocketSession session) {
        final WebSocketSession previous = sessionsByUserId.put(userId, session);
        if (hasDifferentPreviousSession(session, previous)) {
            closeQuietly(previous);
        }
    }

    private static boolean hasDifferentPreviousSession(final WebSocketSession session, final WebSocketSession previous) {
        return previous != null && previous != session;
    }

    public boolean unregister(final Long userId, final WebSocketSession session) {
        return sessionsByUserId.remove(userId, session);
    }

    public Optional<WebSocketSession> find(final Long userId) {
        return Optional.ofNullable(sessionsByUserId.get(userId));
    }

    private void closeQuietly(final WebSocketSession session) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close();
        } catch (final IOException e) {
            log.warn("Failed to close superseded session: {}", e.getMessage());
        }
    }
}
