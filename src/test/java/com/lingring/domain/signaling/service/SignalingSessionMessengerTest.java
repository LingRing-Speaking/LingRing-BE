package com.lingring.domain.signaling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.lingring.domain.signaling.dao.LocalSessionRegistry;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.global.config.ServiceIntegrationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SignalingSessionMessengerTest extends ServiceIntegrationHelper {

    @Autowired
    private SignalingSessionMessenger sessionMessenger;

    @Autowired
    private LocalSessionRegistry sessionRegistry;

    @Nested
    @DisplayName("sendToUser: userId 기반 세션 송신")
    class SendToUser {

        @Test
        @DisplayName("세션이 등록돼있고 열려있으면 sendMessage가 호출된다")
        void sendToUser_whenSessionOpen_sends() throws Exception {
            // given
            final Long userId = 1L;
            final WebSocketSession session = openSession();
            sessionRegistry.register(userId, session);
            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, 2L, userId, null);

            // when
            sessionMessenger.sendToUser(userId, message);

            // then
            then(session).should().sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("세션이 등록되지 않았으면 아무 일도 일어나지 않는다")
        void sendToUser_whenSessionAbsent_doesNothing() {
            // given: 미등록 userId
            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, 2L, 99L, null);

            // when & then: 예외 없이 통과
            sessionMessenger.sendToUser(99L, message);
        }

        @Test
        @DisplayName("세션이 닫혀있으면 sendMessage가 호출되지 않는다")
        void sendToUser_whenSessionClosed_skipsSend() throws Exception {
            // given
            final Long userId = 1L;
            final WebSocketSession session = mock(WebSocketSession.class);
            given(session.isOpen()).willReturn(false);
            sessionRegistry.register(userId, session);
            final SignalingMessage message = new SignalingMessage(
                    SignalingMessageType.OFFER, 2L, userId, null);

            // when
            sessionMessenger.sendToUser(userId, message);

            // then
            then(session).should(never()).sendMessage(any(TextMessage.class));
        }
    }

    private WebSocketSession openSession() {
        final WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        return session;
    }
}
