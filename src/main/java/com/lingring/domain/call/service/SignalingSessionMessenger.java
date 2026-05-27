package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.LocalSessionRegistry;
import com.lingring.domain.call.domain.SignalingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalingSessionMessenger {

    private final LocalSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void sendToUser(final Long userId, final SignalingMessage message) {
        sessionRegistry.find(userId)
                .ifPresent(session -> sendThroughSession(session, message));
    }

    private void sendThroughSession(final WebSocketSession session, final SignalingMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (final Exception e) {
            log.warn("Failed to send signaling message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}
