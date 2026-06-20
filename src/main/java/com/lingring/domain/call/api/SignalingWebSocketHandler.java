package com.lingring.domain.call.api;

import static com.lingring.domain.call.api.SignalingSessionAttributes.*;

import com.lingring.domain.call.dao.LocalSessionRegistry;
import com.lingring.domain.call.domain.SignalingMessage;
import com.lingring.domain.call.facade.SignalingFacade;
import com.lingring.domain.call.service.DisconnectScheduler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final LocalSessionRegistry sessionRegistry;
    private final SignalingFacade signalingFacade;
    private final DisconnectScheduler disconnectScheduler;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        final Long userId = getUserId(session);
        sessionRegistry.register(userId, concurrent(session));
        disconnectScheduler.cancel(userId);
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
        final boolean wasActive = sessionRegistry.unregister(userId, session);
        if (!wasActive) {
            return;
        }
        final UUID roomId = getRoomId(session);
        disconnectScheduler.schedule(userId, () -> {
            if (sessionRegistry.find(userId).isPresent()) {
                return;
            }
            signalingFacade.handleDisconnect(userId, roomId);
        });
    }

    private WebSocketSession concurrent(final WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
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
