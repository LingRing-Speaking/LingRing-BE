package com.lingring.global.auth.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import java.net.MalformedURLException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthJwkSourceConfig {

    @Bean
    public JWKSource<SecurityContext> kakaoJwkSource(
            @Value("${auth.kakao.jwks-uri}") final String jwksUri
    ) throws MalformedURLException {
        return JWKSourceBuilder.create(URI.create(jwksUri).toURL()).build();
    }
}
