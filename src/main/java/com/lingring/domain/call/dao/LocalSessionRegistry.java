package com.lingring.domain.call.dao;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
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

    private boolean hasDifferentPreviousSession(final WebSocketSession session, final WebSocketSession previous) {
        return previous != null && previous != session;
    }

    public boolean unregister(final Long userId, final WebSocketSession session) {
        // 핸들러는 ConcurrentWebSocketSessionDecorator로 감싼 세션을 등록하지만 close 콜백은 raw 세션을 넘긴다.
        // 데코레이터가 getId()를 위임하므로 sessionId로 비교해 같은 연결인지 식별한다.
        final WebSocketSession current = sessionsByUserId.get(userId);
        if (current == null || !current.getId().equals(session.getId())) {
            return false;
        }
        return sessionsByUserId.remove(userId, current);
    }

    public Optional<WebSocketSession> find(final Long userId) {
        return Optional.ofNullable(sessionsByUserId.get(userId));
    }

    public Collection<WebSocketSession> activeSessions() {
        return List.copyOf(sessionsByUserId.values());
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
