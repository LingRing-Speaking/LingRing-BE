package com.lingring.domain.signaling.api;

import com.lingring.domain.signaling.dao.LocalSessionRegistry;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.service.SignalingDispatcher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final LocalSessionRegistry sessionRegistry;
    private final SignalingDispatcher signalingDispatcher;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        final Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        sessionRegistry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
        final Long senderId = userIdOf(session);
        if (senderId == null) {
            return;
        }
        final SignalingMessage parsed = parse(message);
        if (parsed == null) {
            return;
        }
        signalingDispatcher.dispatch(senderId, parsed);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        final Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        sessionRegistry.unregister(userId);
        signalingDispatcher.handleDisconnect(userId, roomIdOf(session));
    }

    private Long userIdOf(final WebSocketSession session) {
        return (Long) session.getAttributes().get(SignalingHandshakeInterceptor.USER_ID_ATTR);
    }

    private UUID roomIdOf(final WebSocketSession session) {
        return (UUID) session.getAttributes().get(SignalingHandshakeInterceptor.ROOM_ID_ATTR);
    }

    private SignalingMessage parse(final TextMessage message) {
        try {
            return objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        } catch (final RuntimeException e) {
            log.warn("Failed to parse signaling message: {}", e.getMessage());
            return null;
        }
    }
}
