package com.lingring.global.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class KakaoIdTokenVerifierTest {

    private static final String EXPECTED_ISSUER = "https://kauth.kakao.com";
    private static final String EXPECTED_AUDIENCE = "test-native-app-key";
    private static final String SUB = "kakao-user-12345";

    private RSAKey signingKey;
    private KakaoIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048)
                .keyID("test-kid")
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        final JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(signingKey));
        verifier = new KakaoIdTokenVerifier(EXPECTED_ISSUER, EXPECTED_AUDIENCE, jwkSource);
    }

    @Nested
    @DisplayName("verify: happy path")
    class HappyPath {

        @Test
        @DisplayName("유효한 id_token이면 sub를 반환한다")
        void verify_whenValid_returnsSub() throws JOSEException {
            // given
            final String token = signToken(claims().build());

            // when
            final String sub = verifier.verify(token);

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
            final String token = signToken(claims().issuer("https://wrong-issuer.com").build());

            // when & then
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("audience가 다르면 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenAudienceMismatch_throws() throws JOSEException {
            // given
            final String token = signToken(claims().audience("wrong-audience").build());

            // when & then
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("만료된 토큰은 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenExpired_throws() throws JOSEException {
            // given
            final Date expired = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
            final String token = signToken(claims().expirationTime(expired).build());

            // when & then
            assertThatThrownBy(() -> verifier.verify(token))
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
                    claims().build()
            );
            signed.sign(new RSASSASigner(otherKey));
            final String token = signed.serialize();

            // when & then
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("형식이 잘못된 문자열은 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenMalformed_throws() {
            // when & then
            assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }

        @Test
        @DisplayName("필수 클레임(sub)이 없으면 UnauthorizedException(INVALID_ID_TOKEN)을 던진다")
        void verify_whenMissingRequiredClaim_throws() throws JOSEException {
            // given
            final JWTClaimsSet missingSub = new JWTClaimsSet.Builder()
                    .issuer(EXPECTED_ISSUER)
                    .audience(EXPECTED_AUDIENCE)
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                    .build();
            final String token = signToken(missingSub);

            // when & then
            assertThatThrownBy(() -> verifier.verify(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ID_TOKEN);
        }
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
                .issuer(EXPECTED_ISSUER)
                .audience(EXPECTED_AUDIENCE)
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
