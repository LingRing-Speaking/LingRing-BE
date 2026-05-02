package com.lingring.domain.signaling.api;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class SignalingHandshakeInterceptor implements HandshakeInterceptor {

    private final CallRepository callRepository;

    @Override
    public boolean beforeHandshake(
            final ServerHttpRequest request,
            final ServerHttpResponse response,
            final WebSocketHandler wsHandler,
            final Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        final HttpServletRequest httpRequest = servletRequest.getServletRequest();

        final Long userId = extractUserId(httpRequest);
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        final UUID roomId = extractRoomId(httpRequest);
        if (roomId == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        final Optional<Call> call = callRepository.findByRoomId(roomId);
        if (call.isEmpty()) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }
        if (!call.get().involves(userId)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        SignalingSessionAttributes.putUserId(attributes, userId);
        SignalingSessionAttributes.putRoomId(attributes, roomId);
        return true;
    }

    @Override
    public void afterHandshake(
            final ServerHttpRequest request,
            final ServerHttpResponse response,
            final WebSocketHandler wsHandler,
            final Exception exception
    ) {
    }

    // TODO(JWT): 토큰 도입 시 이 메서드만 교체. token 추출 + 검증 후 userId 반환.
    private Long extractUserId(final HttpServletRequest request) {
        final String value = request.getParameter(SignalingSessionAttributes.USER_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private UUID extractRoomId(final HttpServletRequest request) {
        final String value = request.getParameter(SignalingSessionAttributes.ROOM_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}
