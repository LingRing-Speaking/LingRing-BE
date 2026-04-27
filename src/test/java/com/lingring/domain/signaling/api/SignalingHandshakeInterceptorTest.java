package com.lingring.domain.signaling.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.domain.Match;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SignalingHandshakeInterceptorTest {

    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 27, 10, 0);

    private MatchRepository matchRepository;
    private SignalingHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        interceptor = new SignalingHandshakeInterceptor(matchRepository);
    }

    @Nested
    @DisplayName("beforeHandshake")
    class BeforeHandshake {

        @Test
        @DisplayName("정상 핸드셰이크 시 attributes에 userId/roomId 저장하고 true 반환")
        void valid_setsAttributesAndReturnsTrue() {
            // given
            given(matchRepository.findByRoomId(ROOM_ID))
                    .willReturn(Optional.of(Match.start(1L, 2L, ROOM_ID, STARTED_AT)));
            final ServerHttpRequest request = buildRequest("userId=1&roomId=" + ROOM_ID);
            final ServerHttpResponse response = buildResponse();
            final Map<String, Object> attributes = new HashMap<>();

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, attributes);

            // then
            assertThat(result).isTrue();
            assertThat(attributes.get(SignalingHandshakeInterceptor.USER_ID_ATTR)).isEqualTo(1L);
            assertThat(attributes.get(SignalingHandshakeInterceptor.ROOM_ID_ATTR)).isEqualTo(ROOM_ID);
        }

        @Test
        @DisplayName("userId 누락 시 401 반환")
        void missingUserId_returns401() {
            // given
            final ServerHttpRequest request = buildRequest("roomId=" + ROOM_ID);
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("userId가 숫자 아님 시 401 반환")
        void invalidUserId_returns401() {
            // given
            final ServerHttpRequest request = buildRequest("userId=abc&roomId=" + ROOM_ID);
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("roomId 누락 시 400 반환")
        void missingRoomId_returns400() {
            // given
            final ServerHttpRequest request = buildRequest("userId=1");
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("roomId가 UUID 형식 아님 시 400 반환")
        void invalidRoomId_returns400() {
            // given
            final ServerHttpRequest request = buildRequest("userId=1&roomId=not-a-uuid");
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Match 미존재 시 404 반환")
        void matchNotFound_returns404() {
            // given
            given(matchRepository.findByRoomId(ROOM_ID)).willReturn(Optional.empty());
            final ServerHttpRequest request = buildRequest("userId=1&roomId=" + ROOM_ID);
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("매칭 비참여자 접근 시 403 반환")
        void notParticipant_returns403() {
            // given
            given(matchRepository.findByRoomId(ROOM_ID))
                    .willReturn(Optional.of(Match.start(1L, 2L, ROOM_ID, STARTED_AT)));
            final ServerHttpRequest request = buildRequest("userId=99&roomId=" + ROOM_ID);
            final MockHttpServletResponse rawResponse = new MockHttpServletResponse();
            final ServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

            // when
            final boolean result = interceptor.beforeHandshake(request, response, null, new HashMap<>());

            // then
            assertThat(result).isFalse();
            assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    private ServerHttpRequest buildRequest(final String queryString) {
        final MockHttpServletRequest rawRequest = new MockHttpServletRequest("GET", "/ws/signaling");
        rawRequest.setQueryString(queryString);
        for (final String pair : queryString.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq > 0) {
                rawRequest.setParameter(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        rawRequest.setRequestURI(URI.create("/ws/signaling").toString());
        return new ServletServerHttpRequest(rawRequest);
    }

    private ServerHttpResponse buildResponse() {
        return new ServletServerHttpResponse(new MockHttpServletResponse());
    }
}
