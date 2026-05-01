package com.lingring.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.domain.Provider;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwksIdTokenVerifierTest {

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_AUDIENCE = "test-native-app-key";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_AUDIENCE = "test-apple-aud";
    private static final String SUB = "user-12345";

    private RSAKey signingKey;
    private JWKSource<SecurityContext> jwkSource;
    private JwksIdTokenVerifier kakaoVerifier;

    @BeforeEach
    void setUp() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048)
                .keyID("test-kid")
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        jwkSource = new ImmutableJWKSet<>(new JWKSet(signingKey));
        kakaoVerifier = new JwksIdTokenVerifier(Provider.KAKAO, KAKAO_ISSUER, KAKAO_AUDIENCE, jwkSource);
    }

    @Nested
    @DisplayName("verify: happy path")
    class HappyPath {

        @Test
        @DisplayName("KAKAO provider로 유효한 id_token을 검증하면 sub를 반환한다")
        void verify_whenValidKakaoToken_returnsSub() throws JOSEException {
            // given
            final String token = signToken(kakaoClaims().build());

            // when
            final String sub = kakaoVerifier.verify(token);

            // then
            assertThat(sub).isEqualTo(SUB);
        }

        @Test
        @DisplayName("APPLE provider로 유효한 id_token을 검증하면 sub를 반환한다")
        void verify_whenValidAppleToken_returnsSub() throws JOSEException {
            // given
            final JwksIdTokenVerifier appleVerifier = new JwksIdTokenVerifier(
                    Provider.APPLE, APPLE_ISSUER, APPLE_AUDIENCE, jwkSource
            );
            final JWTClaimsSet appleClaims = new JWTClaimsSet.Builder()
                    .issuer(APPLE_ISSUER)
                    .audience(APPLE_AUDIENCE)
                    .subject(SUB)
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                    .build();
            final String token = signToken(appleClaims);

            // when
            final String sub = appleVerifier.verify(token);

            // then
            assertThat(sub).isEqualTo(SUB);
        }
    }

    @Nested
    @DisplayName("verify: 검증 실패 → INVALID_ID_TOKEN")
    class InvalidToken {

        @Test
        @DisplayName("issuer가 다르면 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenIssuerMismatch_throws() throws JOSEException {
            // given
            final String token = signToken(kakaoClaims().issuer("https://wrong-issuer.com").build());

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("audience가 다르면 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenAudienceMismatch_throws() throws JOSEException {
            // given
            final String token = signToken(kakaoClaims().audience("wrong-audience").build());

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰은 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenExpired_throws() throws JOSEException {
            // given
            final Date expired = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
            final String token = signToken(kakaoClaims().expirationTime(expired).build());

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰은 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenSignedByDifferentKey_throws() throws JOSEException {
            // given
            final RSAKey otherKey = new RSAKeyGenerator(2048)
                    .keyID("test-kid")
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
            final SignedJWT signed = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(otherKey.getKeyID()).build(),
                    kakaoClaims().build()
            );
            signed.sign(new RSASSASigner(otherKey));
            final String token = signed.serialize();

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("형식이 잘못된 문자열은 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenMalformed_throws() {
            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify("not-a-jwt"))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("필수 클레임(sub)이 없으면 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenMissingRequiredClaim_throws() throws JOSEException {
            // given
            final JWTClaimsSet missingSub = new JWTClaimsSet.Builder()
                    .issuer(KAKAO_ISSUER)
                    .audience(KAKAO_AUDIENCE)
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                    .build();
            final String token = signToken(missingSub);

            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("검증 실패 메시지에 KAKAO provider 이름이 포함된다")
        void verify_whenInvalid_messageContainsKakaoProviderName() {
            // when & then
            assertThatThrownBy(() -> kakaoVerifier.verify("not-a-jwt"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("KAKAO");
        }

        @Test
        @DisplayName("APPLE verifier의 검증 실패 메시지에 APPLE provider 이름이 포함된다")
        void verify_whenAppleInvalid_messageContainsAppleProviderName() {
            // given
            final JwksIdTokenVerifier appleVerifier = new JwksIdTokenVerifier(
                    Provider.APPLE, APPLE_ISSUER, APPLE_AUDIENCE, jwkSource
            );

            // when & then
            assertThatThrownBy(() -> appleVerifier.verify("not-a-jwt"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("APPLE");
        }
    }

    private JWTClaimsSet.Builder kakaoClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(KAKAO_ISSUER)
                .audience(KAKAO_AUDIENCE)
                .subject(SUB)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
    }

    private String signToken(final JWTClaimsSet claims) throws JOSEException {
        final SignedJWT signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claims
        );
        signed.sign(new RSASSASigner(signingKey));
        return signed.serialize();
    }
}
