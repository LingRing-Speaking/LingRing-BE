package com.lingring.domain.call.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class LocalSessionRegistryTest {

    private LocalSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LocalSessionRegistry();
    }

    @Nested
    @DisplayName("register: userId당 단일 세션 슬롯 보장")
    class Register {

        @Test
        @DisplayName("같은 userId로 새 세션이 등록되면 기존 세션은 close된다")
        void register_whenAlreadyRegistered_closesPreviousSession() throws Exception {
            // given
            final Long userId = 1L;
            final WebSocketSession previous = openSession();
            final WebSocketSession current = openSession();
            registry.register(userId, previous);

            // when
            registry.register(userId, current);

            // then
            then(previous).should().close();
            then(current).should(never()).close();
            assertThat(registry.find(userId)).contains(current);
        }

        @Test
        @DisplayName("같은 세션을 다시 register해도 close되지 않는다")
        void register_whenSameSession_doesNotClose() throws Exception {
            // given
            final Long userId = 1L;
            final WebSocketSession session = openSession();
            registry.register(userId, session);

            // when
            registry.register(userId, session);

            // then
            then(session).should(never()).close();
            assertThat(registry.find(userId)).contains(session);
        }
    }

    @Nested
    @DisplayName("unregister: 현재 등록된 세션과 일치할 때만 제거")
    class Unregister {

        @Test
        @DisplayName("등록된 세션과 일치하면 제거하고 true를 반환한다")
        void unregister_whenSessionMatches_removesAndReturnsTrue() {
            // given
            final Long userId = 1L;
            final WebSocketSession session = openSession();
            registry.register(userId, session);

            // when
            final boolean removed = registry.unregister(userId, session);

            // then
            assertThat(removed).isTrue();
            assertThat(registry.find(userId)).isEmpty();
        }

        @Test
        @DisplayName("이미 다른 세션으로 교체된 경우 제거하지 않고 false를 반환한다")
        void unregister_whenSessionSuperseded_keepsCurrentAndReturnsFalse() {
            // given: orphan close 시나리오 — previous가 close되며 unregister를 호출
            final Long userId = 1L;
            final WebSocketSession previous = openSession();
            final WebSocketSession current = openSession();
            registry.register(userId, previous);
            registry.register(userId, current);

            // when
            final boolean removed = registry.unregister(userId, previous);

            // then
            assertThat(removed).isFalse();
            assertThat(registry.find(userId)).contains(current);
        }

        @Test
        @DisplayName("등록된 세션이 없으면 false를 반환한다")
        void unregister_whenAbsent_returnsFalse() {
            // given
            final WebSocketSession session = openSession();

            // when
            final boolean removed = registry.unregister(99L, session);

            // then
            assertThat(removed).isFalse();
        }
    }

    private WebSocketSession openSession() {
        final WebSocketSession session = mock(WebSocketSession.class);
        given(session.isOpen()).willReturn(true);
        return session;
    }
}