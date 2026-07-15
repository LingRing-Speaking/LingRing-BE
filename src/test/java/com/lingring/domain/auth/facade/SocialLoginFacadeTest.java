package com.lingring.domain.auth.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.global.auth.apple.AppleAuthClient;
import com.lingring.global.auth.apple.FakeAppleAuthClient;
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

@Import(SocialLoginFacadeTest.StubIdTokenVerifierConfig.class)
class SocialLoginFacadeTest extends ServiceIntegrationHelper {

    private static final String VALID_ID_TOKEN = "valid-id-token";
    private static final String INVALID_ID_TOKEN = "invalid-id-token";

    @Autowired
    private SocialLoginFacade socialLoginFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatsRepository userStatsRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AppleAuthClient appleAuthClient;

    private FakeAppleAuthClient fakeAppleAuthClient() {
        return (FakeAppleAuthClient) appleAuthClient;
    }

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
            final IdTokenVerifier google = idToken -> {
                if (INVALID_ID_TOKEN.equals(idToken)) {
                    throw new UnauthorizedException(ErrorCode.INVALID_ID_TOKEN, "test stub: invalid");
                }
                return "google-sub-of:" + idToken;
            };
            return new IdTokenVerifiers(Map.of(
                    Provider.KAKAO, kakao,
                    Provider.APPLE, apple,
                    Provider.GOOGLE, google
            ));
        }

        @Bean
        @Primary
        public AppleAuthClient fakeAppleAuthClient() {
            return new FakeAppleAuthClient();
        }
    }

    @Nested
    @DisplayName("socialLogin: 가입 흐름")
    class Signup {

        @Test
        @DisplayName("신규 사용자는 nickname으로 가입되고 access·refresh 토큰을 받는다")
        void socialLogin_whenNewUserWithNickname_createsUserAndIssuesTokens() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, "링링이", null
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(request);

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
        @DisplayName("신규 가입 시 UserStats도 함께 생성된다")
        void socialLogin_whenNewUser_alsoCreatesUserStats() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, "링링이", null
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(request);

            // then
            assertThat(userStatsRepository.findByUserId(response.user().id())).isPresent();
        }

        @Test
        @DisplayName("신규 사용자가 nickname을 보내지 않으면 NICKNAME_REQUIRED 예외가 발생한다")
        void socialLogin_whenNewUserMissingNickname_throwsNicknameRequired() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", VALID_ID_TOKEN, null, null, null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
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
                    "kakao", VALID_ID_TOKEN, null, "링링이", null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
                    .isInstanceOf(NicknameConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    @Nested
    @DisplayName("socialLogin: 로그인 흐름")
    class Existing {

        @Test
        @DisplayName("기존 사용자는 sub로 매칭되어 nickname 없이도 토큰을 받는다")
        void socialLogin_whenExistingUser_returnsTokensWithoutNickname() {
            // given — 사전 가입
            socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "링링이", null)
            );

            // when — 같은 idToken으로 다시 로그인 (nickname 없이)
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, null, null)
            );

            // then
            assertThat(response.user().nickname()).isEqualTo("링링이");
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }
    }

    @Nested
    @DisplayName("socialLogin: Apple provider")
    class Apple {

        @Test
        @DisplayName("APPLE 신규 사용자는 nickname으로 가입되고 토큰을 받는다")
        void socialLogin_whenAppleNewUserWithNickname_createsUserAndIssuesTokens() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "apple", VALID_ID_TOKEN, null, "애플유저", null
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(request);

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
            socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저", null)
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, null, null)
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
                    "apple", INVALID_ID_TOKEN, null, "애플유저", null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("같은 sub라도 provider가 다르면 별도의 사용자로 분리된다")
        void socialLogin_whenSameSubDifferentProvider_createsSeparateUsers() {
            // given — 카카오로 먼저 가입
            final AuthTokenResponse kakaoResponse = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "카카오유저", null)
            );

            // when — 애플로 가입 (다른 nickname 필요 — name unique 제약)
            final AuthTokenResponse appleResponse = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저", null)
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
    @DisplayName("socialLogin: Google provider")
    class Google {

        @Test
        @DisplayName("GOOGLE 신규 사용자는 가입되고 requiresOnboarding 등 기존과 동일한 응답 필드를 받는다")
        void socialLogin_whenGoogleNewUserWithNickname_createsUserAndIssuesTokens() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "google", VALID_ID_TOKEN, null, "구글유저", null
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(request);

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.user().nickname()).isEqualTo("구글유저");
            assertThat(response.user().requiresOnboarding()).isTrue();
            assertThat(response.user().agreedTermsVersion()).isNull();
            assertThat(userRepository.findByProviderAndProviderUserId(
                    Provider.GOOGLE, "google-sub-of:" + VALID_ID_TOKEN
            )).isPresent();
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }

        @Test
        @DisplayName("GOOGLE 기존 사용자는 nickname 없이도 토큰을 받는다")
        void socialLogin_whenGoogleExistingUser_returnsTokensWithoutNickname() {
            // given — 사전 가입
            socialLoginFacade.socialLogin(
                    new SocialLoginRequest("google", VALID_ID_TOKEN, null, "구글유저", null)
            );

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("google", VALID_ID_TOKEN, null, null, null)
            );

            // then
            assertThat(response.user().nickname()).isEqualTo("구글유저");
            assertThat(refreshTokenRepository.exists(response.user().id())).isTrue();
        }

        @Test
        @DisplayName("GOOGLE id_token이 invalid면 UnauthorizedException이 전파된다")
        void socialLogin_whenGoogleIdTokenInvalid_propagatesUnauthorized() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "google", INVALID_ID_TOKEN, null, "구글유저", null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }
    }

    @Nested
    @DisplayName("socialLogin: Apple authorizationCode → refresh_token 캡처")
    class AppleCredentialCapture {

        @Test
        @DisplayName("APPLE + authorizationCode가 있으면 exchange가 호출되고 user에 refresh_token이 저장된다")
        void socialLogin_whenAppleWithAuthCode_exchangesAndPersistsCredential() {
            // given
            fakeAppleAuthClient().reset();
            fakeAppleAuthClient().setNextRefreshToken("apple-rt-123");

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저", "apple-auth-code")
            );

            // then
            assertThat(fakeAppleAuthClient().exchangedCodes()).containsExactly("apple-auth-code");
            final User saved = userRepository.findById(response.user().id()).orElseThrow();
            assertThat(saved.getAppleCredential()).isNotNull();
            assertThat(saved.getAppleCredential().getRefreshToken()).isEqualTo("apple-rt-123");
        }

        @Test
        @DisplayName("APPLE + authorizationCode가 없으면 exchange를 호출하지 않고 credential도 저장되지 않는다")
        void socialLogin_whenAppleWithoutAuthCode_doesNotExchange() {
            // given
            fakeAppleAuthClient().reset();

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저", null)
            );

            // then
            assertThat(fakeAppleAuthClient().exchangedCodes()).isEmpty();
            final User saved = userRepository.findById(response.user().id()).orElseThrow();
            assertThat(saved.getAppleCredential()).isNull();
        }

        @Test
        @DisplayName("APPLE exchange가 실패해도 로그인 자체는 성공하고 credential은 저장되지 않는다")
        void socialLogin_whenExchangeFails_loginStillSucceeds() {
            // given
            fakeAppleAuthClient().reset();
            fakeAppleAuthClient().failNextExchange();

            // when
            final AuthTokenResponse response = socialLoginFacade.socialLogin(
                    new SocialLoginRequest("apple", VALID_ID_TOKEN, null, "애플유저", "apple-auth-code")
            );

            // then
            assertThat(response.accessToken()).isNotBlank();
            final User saved = userRepository.findById(response.user().id()).orElseThrow();
            assertThat(saved.getAppleCredential()).isNull();
        }

        @Test
        @DisplayName("KAKAO + authorizationCode를 보내도 Apple exchange는 호출되지 않는다")
        void socialLogin_whenKakaoWithAuthCode_doesNotInvokeAppleExchange() {
            // given
            fakeAppleAuthClient().reset();

            // when
            socialLoginFacade.socialLogin(
                    new SocialLoginRequest("kakao", VALID_ID_TOKEN, null, "카카오유저", "should-be-ignored")
            );

            // then
            assertThat(fakeAppleAuthClient().exchangedCodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("socialLogin: 입력 검증")
    class Validation {

        @Test
        @DisplayName("provider가 지원되지 않으면 NOT_SUPPORTED 예외가 발생한다")
        void socialLogin_whenUnsupportedProvider_throwsNotSupported() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "facebook", VALID_ID_TOKEN, null, "닉네임", null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("id_token 검증이 실패하면 IdTokenVerifier의 예외가 그대로 전파된다")
        void socialLogin_whenIdTokenInvalid_propagatesVerifierException() {
            // given
            final SocialLoginRequest request = new SocialLoginRequest(
                    "kakao", INVALID_ID_TOKEN, null, "닉네임", null
            );

            // when & then
            assertThatThrownBy(() -> socialLoginFacade.socialLogin(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }
    }
}
