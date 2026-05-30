package com.lingring.infrastructure.redis;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRedisRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RefreshTokenRedisRepository(
            final StringRedisTemplate redisTemplate,
            @Value("${auth.jwt.refresh-ttl}") final Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    @Override
    public void save(final Long userId, final String refreshToken) {
        redisTemplate.opsForValue().set(key(userId), hashOf(refreshToken), ttl);
    }

    @Override
    public boolean exists(final Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    @Override
    public boolean matches(final Long userId, final String refreshToken) {
        final String stored = redisTemplate.opsForValue().get(key(userId));
        if (stored == null) {
            return false;
        }
        return stored.equals(hashOf(refreshToken));
    }

    @Override
    public void deleteByUserId(final Long userId) {
        redisTemplate.delete(key(userId));
    }

    private static String key(final Long userId) {
        return KEY_PREFIX + userId;
    }

    private static String hashOf(final String token) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}