package com.lingring.domain.signaling.api;

import static com.lingring.domain.signaling.api.SignalingSessionAttributes.*;

import com.lingring.domain.signaling.dao.LocalSessionRegistry;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.facade.SignalingFacade;
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
    private final SignalingFacade signalingFacade;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        sessionRegistry.register(getUserId(session), session);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
        final SignalingMessage parsed = parse(message);
        if (parsed == null) {
            return;
        }
        signalingFacade.dispatch(getUserId(session), getRoomId(session), parsed);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        final Long userId = getUserId(session);
        sessionRegistry.unregister(userId);
        signalingFacade.handleDisconnect(userId, getRoomId(session));
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
