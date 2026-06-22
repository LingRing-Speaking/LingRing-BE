package com.lingring.global.config;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:5173",
            "http://localhost:4173",
            "https://lingring.site",
            "capacitor://*",
            "https://dev-lingring.site",
            "http://dev-lingring.site"
    );

    private static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    );

    private static final long MAX_AGE_SECONDS = 3600L;

    // 핸들러 레벨(addCorsMappings) 대신 서블릿 레벨 CorsFilter 로 등록한다.
    // JwtAuthenticationFilter 가 인증 실패 시 DispatcherServlet 전에 short-circuit(401) 하므로,
    // 핸들러 레벨 CORS 로는 그 에러 응답에 CORS 헤더가 붙지 않는다. CorsFilter 를 가장 앞단에 두어
    // preflight·성공·필터 단계 401 등 모든 응답에 CORS 헤더가 붙도록 보장한다.
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(MAX_AGE_SECONDS);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        final FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
