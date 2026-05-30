package com.lingring.global.auth.config;

import com.lingring.domain.user.domain.Provider;
import com.lingring.global.auth.jwt.IdTokenVerifier;
import com.lingring.global.auth.jwt.IdTokenVerifiers;
import com.lingring.global.auth.jwt.JwksIdTokenVerifier;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdTokenVerifierConfig {

    @Bean
    public JWKSource<SecurityContext> kakaoJwkSource(
            @Value("${auth.kakao.jwks-uri}") final String jwksUri
    ) throws MalformedURLException {
        return JWKSourceBuilder.create(URI.create(jwksUri).toURL()).build();
    }

    @Bean
    public JWKSource<SecurityContext> appleJwkSource(
            @Value("${auth.apple.jwks-uri}") final String jwksUri
    ) throws MalformedURLException {
        return JWKSourceBuilder.create(URI.create(jwksUri).toURL()).build();
    }

    @Bean
    public IdTokenVerifier kakaoIdTokenVerifier(
            @Value("${auth.kakao.iss}") final String iss,
            @Value("${auth.kakao.aud}") final String aud,
            @Qualifier("kakaoJwkSource") final JWKSource<SecurityContext> kakaoJwkSource
    ) {
        return new JwksIdTokenVerifier(Provider.KAKAO, iss, aud, kakaoJwkSource);
    }

    @Bean
    public IdTokenVerifier appleIdTokenVerifier(
            @Value("${auth.apple.iss}") final String iss,
            @Value("${auth.apple.aud}") final String aud,
            @Qualifier("appleJwkSource") final JWKSource<SecurityContext> appleJwkSource
    ) {
        return new JwksIdTokenVerifier(Provider.APPLE, iss, aud, appleJwkSource);
    }

    @Bean
    public IdTokenVerifiers idTokenVerifiers(
            @Qualifier("kakaoIdTokenVerifier") final IdTokenVerifier kakaoIdTokenVerifier,
            @Qualifier("appleIdTokenVerifier") final IdTokenVerifier appleIdTokenVerifier
    ) {
        return new IdTokenVerifiers(Map.of(
                Provider.KAKAO, kakaoIdTokenVerifier,
                Provider.APPLE, appleIdTokenVerifier
        ));
    }
}
