package com.lingring.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.auth.facade.SocialLoginFacade;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.IdTokenVerifiers;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(AuthServiceTest.StubIdTokenVerifierConfig.class)
class AuthServiceTest extends ServiceIntegrationHelper {

    private static final String VALID_ID_TOKEN = "valid-id-token";
    private static final String INVALID_ID_TOKEN = "invalid-id-token";

    @Autowired
    private AuthService authService;

    @Autowired
    private SocialLoginFacade socialLoginFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @TestConfiguration
    static class StubIdTokenVerifierConfig {

        @Bean
        @Primary
        public IdTokenVerifiers stubIdTokenVerifiers() {
            final IdTokenVerifier kakao = idToken -> {
                if (INVALID_ID_TOKEN.equals(idToken)) {
                    throw new UnauthorizedException(ErrorCode.INVALID_ID_TOKEN, "test stub: invalid");
                }
                return "kakao-sub-of:" + idToken;
            };
            return new IdTokenVerifiers(Map.of(Provider.KAKAO, kakao));
        }
    }

    @Nested
    @DisplayName("verifyIdToken: id_token 검증")
    class VerifyIdToken {

        @Test
        @DisplayName("유효한 id_token이면 provider와 providerUserId를 반환한다")
        void verifyIdToken_whenValid_returnsProviderAndSub() {
            // when
            final VerifiedIdToken verified = authService.verifyIdToken("kakao", VALID_ID_TOKEN);

            // then
            assertThat(verified.provider()).isEqualTo(Provider.KAKAO);
            assertThat(verified.providerUserId()).isEqualTo("kakao-sub-of:" + VALID_ID_TOKEN);
        }

        @Test
        @DisplayName("id_token이 invalid면 UnauthorizedException이 전파된다")
        void verifyIdToken_whenInvalid_throwsUnauthorized() {
            // when & then
            assertThatThrownBy(() -> authService.verifyIdToken("kakao", INVALID_ID_TOKEN))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("provider가 지원되지 않으면 NOT_SUPPORTED 예외가 발생한다")
        void verifyIdToken_whenUnsupportedProvider_throwsNotSupported() {
            // when & then
            assertThatThrownBy(() -> authService.verifyIdToken("facebook", VALID_ID_TOKEN))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }
    }

    @Nested
    @DisplayName("issueTokensFor: User로 access·refresh 토큰을 발급한다")
    class IssueTokensFor {

        @Test
        @DisplayName("토큰 쌍을 발급하고 Redis에 refresh를 저장한다")
        void issueTokensFor_whenCalled_returnsTokensAndPersistsRefresh() {
            // given
            final User user = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-sub", new Name("링링이"), null)
            );

            // when
            final AuthTokenResponse response = authService.issueTokensFor(user);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().id()).isEqualTo(user.getId());
            assertThat(response.user().nickname()).isEqualTo("링링이");
            assertThat(refreshTokenRepository.exists(user.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("refresh: 토큰 회전")
    class Refresh {

        @Test
        @DisplayName("유효한 refresh token으로 새 토큰 쌍을 발급한다")
        void refresh_whenValidToken_issuesNewPair() {
            // given
            final AuthTokenResponse first = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );

            // when
            final TokenPairResponse second = authService.refresh(first.refreshToken());

            // then
            assertThat(second.accessToken()).isNotBlank();
            assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
            assertThat(refreshTokenRepository.exists(first.user().id())).isTrue();
        }

        @Test
        @DisplayName("토큰은 유효하지만 사용자가 삭제됐다면 INVALID_TOKEN을 던지고 세션을 무효화한다")
        void refresh_whenUserDeleted_throwsInvalidTokenAndInvalidates() {
            // given
            final AuthTokenResponse first = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );
            userRepository.deleteById(first.user().id());

            // when & then
            assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
            assertThat(refreshTokenRepository.exists(first.user().id())).isFalse();
        }

        @Test
        @DisplayName("이미 회전된(stale) refresh token을 재사용하면 모든 세션이 무효화되고 INVALID_TOKEN을 던진다")
        void refresh_whenStaleTokenReused_invalidatesAllAndThrows() {
            // given
            final AuthTokenResponse first = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );
            authService.refresh(first.refreshToken());

            // when & then — 옛 refresh token으로 다시 시도
            assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
            assertThat(refreshTokenRepository.exists(first.user().id())).isFalse();
        }

        @Test
        @DisplayName("Redis에 저장된 refresh가 없으면(로그아웃 후) INVALID_TOKEN을 던진다")
        void refresh_whenNoStoredToken_throws() {
            // given
            final AuthTokenResponse first = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );
            authService.logout(first.user().id());

            // when & then
            assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("access token을 refresh로 보내면 INVALID_TOKEN을 던진다 (타입 검증)")
        void refresh_whenAccessTokenGiven_throws() {
            // given
            final AuthTokenResponse first = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );

            // when & then
            assertThatThrownBy(() -> authService.refresh(first.accessToken()))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("logout은 Redis의 refresh token을 삭제한다")
        void logout_deletesStoredRefreshToken() {
            // given
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();

            // when
            authService.logout(response.user().id());

            // then
            assertThat(refreshTokenRepository.exists(response.user().id())).isFalse();
        }

        @Test
        @DisplayName("저장된 refresh가 없는 userId로 logout해도 예외 없이 동작한다 (멱등)")
        void logout_isIdempotent() {
            // given
            final Long unknownUserId = 9_999_999L;

            // when & then — should not throw
            authService.logout(unknownUserId);
        }
    }
}
