package com.lingring.global.auth.jwt;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import com.lingring.global.util.DateTimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final long CLOCK_SKEW_SECONDS = 30L;

    private final DateTimeProvider dateTimeProvider;
    private final Clock jjwtClock;
    private final SecretKey signingKey;
    private final long accessTtlMillis;
    private final long refreshTtlMillis;

    public JwtProvider(
            final DateTimeProvider dateTimeProvider,
            @Value("${auth.jwt.secret}") final String secret,
            @Value("${auth.jwt.access-ttl}") final Duration accessTtl,
            @Value("${auth.jwt.refresh-ttl}") final Duration refreshTtl
    ) {
        this.dateTimeProvider = dateTimeProvider;
        this.jjwtClock = () -> Date.from(dateTimeProvider.now().atZone(ZONE).toInstant());
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = accessTtl.toMillis();
        this.refreshTtlMillis = refreshTtl.toMillis();
    }

    public String issueAccessToken(final Long userId) {
        return issue(userId, TOKEN_TYPE_ACCESS, accessTtlMillis);
    }

    public String issueRefreshToken(final Long userId) {
        return issue(userId, TOKEN_TYPE_REFRESH, refreshTtlMillis);
    }

    public Long parseAccessToken(final String token) {
        return parseAndVerify(token, TOKEN_TYPE_ACCESS);
    }

    public Long parseRefreshToken(final String token) {
        return parseAndVerify(token, TOKEN_TYPE_REFRESH);
    }

    private String issue(final Long userId, final String type, final long ttlMillis) {
        final Instant now = dateTimeProvider.now().atZone(ZONE).toInstant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private Long parseAndVerify(final String token, final String expectedType) {
        try {
            final Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .clock(jjwtClock)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            final String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(type)) {
                throw new UnauthorizedException(
                        ErrorCode.INVALID_TOKEN,
                        "토큰 타입이 일치하지 않습니다. expected=%s, actual=%s".formatted(expectedType, type)
                );
            }
            return Long.valueOf(claims.getSubject());
        } catch (final JwtException ex) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "유효하지 않은 토큰입니다: %s".formatted(ex.getMessage())
            );
        }
    }
}
