package com.lingring.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-min-32-bytes-required-for-hs256-here";
    private static final String DIFFERENT_SECRET = "different-secret-also-min-32-bytes-required-here";
    private static final Duration ACCESS_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final Long USER_ID = 123L;

    private FixedDateTimeProvider dateTimeProvider;
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        dateTimeProvider = new FixedDateTimeProvider(NOW);
        jwtProvider = new JwtProvider(dateTimeProvider, SECRET, ACCESS_TTL, REFRESH_TTL);
    }

    @Nested
    @DisplayName("access token 발급/파싱")
    class AccessToken {

        @Test
        @DisplayName("issueAccessToken으로 발급한 토큰을 parseAccessToken으로 파싱하면 userId가 반환된다")
        void issueAccessToken_canBeParsedBackToUserId() {
            // given
            final String token = jwtProvider.issueAccessToken(USER_ID);

            // when
            final Long parsed = jwtProvider.parseAccessToken(token);

            // then
            assertThat(parsed).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("access TTL이 지난 후 파싱하면 UnauthorizedException이 발생한다")
        void parseAccessToken_whenExpired_throws() {
            // given
            final String token = jwtProvider.issueAccessToken(USER_ID);
            dateTimeProvider.setFixedTime(NOW.plusHours(2));

            // when & then
            assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰을 파싱하면 UnauthorizedException이 발생한다")
        void parseAccessToken_whenSignedWithDifferentKey_throws() {
            // given
            final JwtProvider other =
                    new JwtProvider(dateTimeProvider, DIFFERENT_SECRET, ACCESS_TTL, REFRESH_TTL);
            final String token = other.issueAccessToken(USER_ID);

            // when & then
            assertThatThrownBy(() -> jwtProvider.parseAccessToken(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("형식이 잘못된 문자열을 파싱하면 UnauthorizedException이 발생한다")
        void parseAccessToken_whenMalformed_throws() {
            // when & then
            assertThatThrownBy(() -> jwtProvider.parseAccessToken("not-a-jwt"))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("refresh token 발급/파싱")
    class RefreshToken {

        @Test
        @DisplayName("issueRefreshToken으로 발급한 토큰을 parseRefreshToken으로 파싱하면 userId가 반환된다")
        void issueRefreshToken_canBeParsedBackToUserId() {
            // given
            final String token = jwtProvider.issueRefreshToken(USER_ID);

            // when
            final Long parsed = jwtProvider.parseRefreshToken(token);

            // then
            assertThat(parsed).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("토큰 타입 교차 검증")
    class TokenTypeMismatch {

        @Test
        @DisplayName("refresh 토큰을 parseAccessToken으로 파싱하면 UnauthorizedException이 발생한다")
        void parseAccessToken_whenRefreshTokenGiven_throws() {
            // given
            final String refresh = jwtProvider.issueRefreshToken(USER_ID);

            // when & then
            assertThatThrownBy(() -> jwtProvider.parseAccessToken(refresh))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("access 토큰을 parseRefreshToken으로 파싱하면 UnauthorizedException이 발생한다")
        void parseRefreshToken_whenAccessTokenGiven_throws() {
            // given
            final String access = jwtProvider.issueAccessToken(USER_ID);

            // when & then
            assertThatThrownBy(() -> jwtProvider.parseRefreshToken(access))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }
}
