package com.lingring.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.MeResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.auth.exception.NicknameConflictException;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.IdTokenVerifiers;
import com.lingring.global.auth.jwt.JwtProvider;
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
    private static final String OTHER_VALID_ID_TOKEN = "valid-id-token-2";
    private static final String INVALID_ID_TOKEN = "invalid-id-token";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

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
            final IdTokenVerifier apple = idToken -> {
                if (INVALID_ID_TOKEN.equals(idToken)) {
                    throw new UnauthorizedException(ErrorCode.INVALID_ID_TOKEN, "test stub: invalid");
                }
                return "apple-sub-of:" + idToken;
            };
            return new IdTokenVerifiers(Map.of(
                    Provider.KAKAO, kakao,
                    Provider.APPLE, apple
            ));
        }
    }

    @Nested
    @DisplayName("socialLogin: 가입 흐름")
    class SocialLoginSignup {

        @Test
        @DisplayName("신규 사용자는 nickname으로 가입되고 access·refresh 토큰을 받는다")
        void socialLogin_whenNewUserWithNickname_createsUserAndIssuesTokens() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, "링링이"
            );

            // when
            final AuthTokenResponse response = authService.socialLogin(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().nickname()).isEqualTo("링링이");
            assertThat(userRepository.findByProviderAndProviderUserId(
                    Provider.KAKAO, "kakao-sub-of:" + VALID_ID_TOKEN
            )).isPresent();
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }

        @Test
        @DisplayName("신규 사용자가 nickname을 보내지 않으면 NICKNAME_REQUIRED 예외가 발생한다")
        void socialLogin_whenNewUserMissingNickname_throwsNicknameRequired() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, null
            );

            // when & then
            assertThatThrownBy(() -> authService.socialLogin(request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_REQUIRED);
        }

        @Test
        @DisplayName("이미 사용 중인 nickname으로 신규 가입 시 NICKNAME_CONFLICT 예외가 발생한다")
        void socialLogin_whenNicknameAlreadyTaken_throwsNicknameConflict() {
            // given — pre-existing user with "링링이"
            userRepository.save(User.createFromOAuth(
                    Provider.KAKAO, "existing-sub", new Name("링링이"), null
            ));
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, "링링이"
            );

            // when & then
            assertThatThrownBy(() -> authService.socialLogin(request))
                    .isInstanceOf(NicknameConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    @Nested
    @DisplayName("socialLogin: 로그인 흐름")
    class SocialLoginExisting {

        @Test
        @DisplayName("기존 사용자는 sub로 매칭되어 nickname 없이도 토큰을 받는다")
        void socialLogin_whenExistingUser_returnsTokensWithoutNickname() {
            // given — 사전 가입
            authService.socialLogin(new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이"));

            // when — 같은 idToken으로 다시 로그인 (nickname 없이)
            final AuthTokenResponse response = authService.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, null)
            );

            // then
            assertThat(response.user().nickname()).isEqualTo("링링이");
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }
    }

    @Nested
    @DisplayName("socialLogin: Apple provider")
    class SocialLoginApple {

        @Test
        @DisplayName("APPLE 신규 사용자는 nickname으로 가입되고 토큰을 받는다")
        void socialLogin_whenAppleNewUserWithNickname_createsUserAndIssuesTokens() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "apple", VALID_ID_TOKEN, null, "애플유저"
            );

            // when
            final AuthTokenResponse response = authService.socialLogin(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().nickname()).isEqualTo("애플유저");
            assertThat(userRepository.findByProviderAndProviderUserId(
                    Provider.APPLE, "apple-sub-of:" + VALID_ID_TOKEN
            )).isPresent();
        }

        @Test
        @DisplayName("APPLE 기존 사용자는 nickname 없이도 토큰을 받는다")
        void socialLogin_whenAppleExistingUser_returnsTokensWithoutNickname() {
            // given — 사전 가입
            authService.socialLogin(new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저"));

            // when
            final AuthTokenResponse response = authService.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, null)
            );

            // then
            assertThat(response.user().nickname()).isEqualTo("애플유저");
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }

        @Test
        @DisplayName("APPLE id_token이 invalid면 UnauthorizedException이 전파된다")
        void socialLogin_whenAppleIdTokenInvalid_propagatesUnauthorized() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "apple", INVALID_ID_TOKEN, null, "애플유저"
            );

            // when & then
            assertThatThrownBy(() -> authService.socialLogin(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("같은 sub라도 provider가 다르면 별도의 사용자로 분리된다")
        void socialLogin_whenSameSubDifferentProvider_createsSeparateUsers() {
            // given — 카카오로 먼저 가입
            final AuthTokenResponse kakaoResponse = authService.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "카카오유저")
            );

            // when — 애플로 가입 (다른 nickname 필요 — name unique 제약)
            final AuthTokenResponse appleResponse = authService.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저")
            );

            // then
            assertThat(appleResponse.user().id()).isNotEqualTo(kakaoResponse.user().id());
            assertThat(userRepository.findByProviderAndProviderUserId(
                    Provider.KAKAO, "kakao-sub-of:" + VALID_ID_TOKEN
            )).isPresent();
            assertThat(userRepository.findByProviderAndProviderUserId(
                    Provider.APPLE, "apple-sub-of:" + VALID_ID_TOKEN
            )).isPresent();
        }
    }

    @Nested
    @DisplayName("socialLogin: 입력 검증")
    class SocialLoginValidation {

        @Test
        @DisplayName("provider가 지원되지 않으면 NOT_SUPPORTED 예외가 발생한다")
        void socialLogin_whenUnsupportedProvider_throwsNotSupported() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "facebook", VALID_ID_TOKEN, null, "닉네임"
            );

            // when & then
            assertThatThrownBy(() -> authService.socialLogin(request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("id_token 검증이 실패하면 IdTokenVerifier의 예외가 그대로 전파된다")
        void socialLogin_whenIdTokenInvalid_propagatesVerifierException() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", INVALID_ID_TOKEN, null, "닉네임"
            );

            // when & then
            assertThatThrownBy(() -> authService.socialLogin(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }
    }

    @Nested
    @DisplayName("refresh: 토큰 회전")
    class Refresh {

        @Test
        @DisplayName("유효한 refresh token으로 새 토큰 쌍을 발급한다")
        void refresh_whenValidToken_issuesNewPair() {
            // given
            final AuthTokenResponse first = authService.socialLogin(
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
            final AuthTokenResponse first = authService.socialLogin(
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
            final AuthTokenResponse first = authService.socialLogin(
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
            final AuthTokenResponse first = authService.socialLogin(
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
            final AuthTokenResponse first = authService.socialLogin(
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
            final AuthTokenResponse response = authService.socialLogin(
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

    @Nested
    @DisplayName("me: 내 정보 조회")
    class Me {

        @Test
        @DisplayName("기존 사용자는 id와 nickname을 반환한다")
        void me_whenUserExists_returnsIdAndNickname() {
            // given
            final AuthTokenResponse signup = authService.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이")
            );

            // when
            final MeResponse response = authService.me(signup.user().id());

            // then
            assertThat(response.id()).isEqualTo(signup.user().id());
            assertThat(response.nickname()).isEqualTo("링링이");
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 INVALID_TOKEN을 던진다 (401)")
        void me_whenUserNotFound_throwsInvalidToken() {
            // given
            final Long unknownUserId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> authService.me(unknownUserId))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }
}
