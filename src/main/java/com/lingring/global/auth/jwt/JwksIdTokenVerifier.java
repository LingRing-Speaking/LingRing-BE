package com.lingring.global.auth.jwt;

import com.lingring.domain.user.domain.Provider;
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
import java.util.HashSet;
import java.util.Set;

public class JwksIdTokenVerifier implements IdTokenVerifier {

    private static final Set<String> REQUIRED_CLAIMS = Set.of("sub", "iat", "exp", "iss");

    private final Provider provider;
    private final Set<String> allowedIssuers;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwksIdTokenVerifier(
            final Provider provider,
            final Set<String> allowedIssuers,
            final Set<String> allowedAudiences,
            final JWKSource<SecurityContext> jwkSource
    ) {
        this.provider = provider;
        this.allowedIssuers = Set.copyOf(allowedIssuers);
        this.processor = buildProcessor(allowedAudiences, jwkSource);
    }

    @Override
    public String verify(final String idToken) {
        try {
            final JWTClaimsSet claims = processor.process(idToken, null);
            verifyIssuer(claims.getIssuer());
            return claims.getSubject();
        } catch (final RemoteKeySourceException ex) {
            throw new IdpUnavailableException(
                    ErrorCode.IDP_UNAVAILABLE,
                    "%s JWKS 조회에 실패했습니다: %s".formatted(provider.name(), ex.getMessage())
            );
        } catch (final BadJOSEException | JOSEException | ParseException ex) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_ID_TOKEN,
                    "%s id_token 검증 실패: %s".formatted(provider.name(), ex.getMessage())
            );
        }
    }

    private void verifyIssuer(final String issuer) {
        if (!allowedIssuers.contains(issuer)) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_ID_TOKEN,
                    "%s id_token 검증 실패: 허용되지 않은 issuer입니다: %s".formatted(provider.name(), issuer)
            );
        }
    }

    // DefaultJWTClaimsVerifier는 issuer 복수 허용을 지원하지 않아 iss는 verifyIssuer에서 별도 검사
    private ConfigurableJWTProcessor<SecurityContext> buildProcessor(
            final Set<String> allowedAudiences,
            final JWKSource<SecurityContext> jwkSource
    ) {
        final DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        final JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        jwtProcessor.setJWSKeySelector(keySelector);
        // DefaultJWTClaimsVerifier가 내부에서 contains(null)을 호출하므로 immutable Set 금지
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new HashSet<>(allowedAudiences),
                null,
                REQUIRED_CLAIMS,
                null
        ));
        return jwtProcessor;
    }
}
