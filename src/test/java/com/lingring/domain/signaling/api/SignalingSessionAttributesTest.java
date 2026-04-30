package com.lingring.domain.signaling.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class SignalingSessionAttributesTest {

    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("putUserId / getUserId")
    class UserId {

        @Test
        @DisplayName("put 후 동일한 세션에서 get하면 동일 값을 반환한다")
        void put_thenGet_returnsSameValue() {
            // given
            final Map<String, Object> attributes = new HashMap<>();
            SignalingSessionAttributes.putUserId(attributes, 42L);
            final WebSocketSession session = sessionWithAttributes(attributes);

            // when
            final Long actual = SignalingSessionAttributes.getUserId(session);

            // then
            assertThat(actual).isEqualTo(42L);
        }

        @Test
        @DisplayName("put 시 userId가 null이면 NPE를 던진다")
        void put_whenNull_throwsNpe() {
            // given
            final Map<String, Object> attributes = new HashMap<>();

            // when & then
            assertThatThrownBy(() -> SignalingSessionAttributes.putUserId(attributes, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("get 시 세션에 userId가 없으면 인바리언트 위반 메시지로 NPE를 던진다")
        void get_whenMissing_throwsInvariantViolation() {
            // given
            final WebSocketSession session = sessionWithAttributes(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> SignalingSessionAttributes.getUserId(session))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("핸드셰이크 인터셉터 인바리언트 위반");
        }
    }

    @Nested
    @DisplayName("putRoomId / getRoomId")
    class RoomId {

        @Test
        @DisplayName("put 후 동일한 세션에서 get하면 동일 값을 반환한다")
        void put_thenGet_returnsSameValue() {
            // given
            final Map<String, Object> attributes = new HashMap<>();
            SignalingSessionAttributes.putRoomId(attributes, ROOM_ID);
            final WebSocketSession session = sessionWithAttributes(attributes);

            // when
            final UUID actual = SignalingSessionAttributes.getRoomId(session);

            // then
            assertThat(actual).isEqualTo(ROOM_ID);
        }

        @Test
        @DisplayName("put 시 roomId가 null이면 NPE를 던진다")
        void put_whenNull_throwsNpe() {
            // given
            final Map<String, Object> attributes = new HashMap<>();

            // when & then
            assertThatThrownBy(() -> SignalingSessionAttributes.putRoomId(attributes, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("roomId");
        }

        @Test
        @DisplayName("get 시 세션에 roomId가 없으면 인바리언트 위반 메시지로 NPE를 던진다")
        void get_whenMissing_throwsInvariantViolation() {
            // given
            final WebSocketSession session = sessionWithAttributes(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> SignalingSessionAttributes.getRoomId(session))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("핸드셰이크 인터셉터 인바리언트 위반");
        }
    }

    private WebSocketSession sessionWithAttributes(final Map<String, Object> attributes) {
        final WebSocketSession session = mock(WebSocketSession.class);
        given(session.getAttributes()).willReturn(attributes);
        return session;
    }
}
