package com.lingring.global.auth.jwt;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.IdpUnavailableException;
import com.lingring.global.error.exception.UnauthorizedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.text.ParseException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoIdTokenVerifier implements IdTokenVerifier {

    private static final Set<String> REQUIRED_CLAIMS = Set.of("sub", "iat", "exp");

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public KakaoIdTokenVerifier(
            @Value("${auth.kakao.iss}") final String iss,
            @Value("${auth.kakao.aud}") final String aud,
            final JWKSource<SecurityContext> kakaoJwkSource
    ) {
        this.processor = buildProcessor(iss, aud, kakaoJwkSource);
    }

    @Override
    public String verify(final String idToken) {
        try {
            final JWTClaimsSet claims = processor.process(idToken, null);
            return claims.getSubject();
        } catch (final RemoteKeySourceException ex) {
            throw new IdpUnavailableException(
                    ErrorCode.IDP_UNAVAILABLE,
                    "카카오 JWKS 조회에 실패했습니다: %s".formatted(ex.getMessage())
            );
        } catch (final BadJOSEException | JOSEException | ParseException ex) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_ID_TOKEN,
                    "id_token 검증 실패: %s".formatted(ex.getMessage())
            );
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> buildProcessor(
            final String iss,
            final String aud,
            final JWKSource<SecurityContext> jwkSource
    ) {
        final DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        final JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        jwtProcessor.setJWSKeySelector(keySelector);
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                aud,
                new JWTClaimsSet.Builder().issuer(iss).build(),
                REQUIRED_CLAIMS
        ));
        return jwtProcessor;
    }
}
