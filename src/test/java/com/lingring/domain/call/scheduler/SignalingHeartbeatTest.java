package com.lingring.domain.call.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.lingring.domain.call.dao.LocalSessionRegistry;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

class SignalingHeartbeatTest {

    private LocalSessionRegistry sessionRegistry;
    private SignalingHeartbeat heartbeat;

    @BeforeEach
    void setUp() {
        sessionRegistry = new LocalSessionRegistry();
        // 테스트는 호출 스레드에서 동기 실행해 핑 전송을 결정적으로 검증한다.
        heartbeat = new SignalingHeartbeat(sessionRegistry, Runnable::run);
    }

    @Nested
    @DisplayName("sendPings: 열린 세션에 ping 프레임 전송")
    class SendPings {

        @Test
        @DisplayName("열린 모든 세션에 ping 프레임을 전송한다")
        void sendPings_sendsPingToEveryOpenSession() throws Exception {
            // given
            final WebSocketSession first = openSession();
            final WebSocketSession second = openSession();
            sessionRegistry.register(1L, first);
            sessionRegistry.register(2L, second);

            // when
            heartbeat.sendPings();

            // then
            then(first).should().sendMessage(any(PingMessage.class));
            then(second).should().sendMessage(any(PingMessage.class));
        }

        @Test
        @DisplayName("닫힌 세션에는 ping을 보내지 않는다")
        void sendPings_skipsClosedSession() throws Exception {
            // given
            final WebSocketSession closed = mock(WebSocketSession.class);
            given(closed.isOpen()).willReturn(false);
            sessionRegistry.register(1L, closed);

            // when
            heartbeat.sendPings();

            // then
            then(closed).should(never()).sendMessage(any());
        }

        @Test
        @DisplayName("한 세션 전송이 실패해도 나머지 세션에는 계속 전송한다")
        void sendPings_whenOneSessionFails_continuesWithOthers() throws Exception {
            // given
            final WebSocketSession failing = openSession();
            doThrow(new IOException("broken pipe")).when(failing).sendMessage(any(PingMessage.class));
            final WebSocketSession healthy = openSession();
            sessionRegistry.register(1L, failing);
            sessionRegistry.register(2L, healthy);

            // when
            heartbeat.sendPings();

            // then
            then(healthy).should().sendMessage(any(PingMessage.class));
        }
    }

    private WebSocketSession openSession() {
        final WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        return session;
    }
}
