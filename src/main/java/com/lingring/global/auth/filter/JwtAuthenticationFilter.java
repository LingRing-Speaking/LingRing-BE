package com.lingring.global.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingring.global.auth.context.AuthContext;
import com.lingring.global.auth.jwt.JwtProvider;
import com.lingring.global.common.response.ApiResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/**",
            "/ws/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/api-docs/**",
            "/v3/api-docs/**"
    );

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(final JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (isPublic(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final Long userId = authenticate(request);
            AuthContext.set(userId);
            filterChain.doFilter(request, response);
        } catch (final UnauthorizedException ex) {
            writeError(response, ex.getErrorCode());
        } finally {
            AuthContext.clear();
        }
    }

    private Long authenticate(final HttpServletRequest request) {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "Authorization 헤더가 없거나 Bearer 형식이 아닙니다."
            );
        }
        final String token = header.substring(BEARER_PREFIX.length());
        return jwtProvider.parseAccessToken(token);
    }

    private void writeError(final HttpServletResponse response, final ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }

    private boolean isPublic(final String uri) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
    }
}
