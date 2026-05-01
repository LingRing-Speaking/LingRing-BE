package com.lingring.global.auth.config;

import com.lingring.global.auth.filter.JwtAuthenticationFilter;
import com.lingring.global.auth.jwt.JwtProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterConfig {

    private static final int FILTER_ORDER = 2;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(
            final JwtProvider jwtProvider
    ) {
        final FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(new JwtAuthenticationFilter(jwtProvider));
        registration.setOrder(FILTER_ORDER);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
