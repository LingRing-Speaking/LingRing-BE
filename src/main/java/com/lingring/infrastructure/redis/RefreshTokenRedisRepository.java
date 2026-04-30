package com.lingring.infrastructure.redis;

import com.lingring.domain.auth.dao.RefreshTokenRepository;
import java.time.Duration;
import java.util.Optional;
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
    public void save(final Long userId, final String tokenHash) {
        redisTemplate.opsForValue().set(key(userId), tokenHash, ttl);
    }

    @Override
    public Optional<String> findByUserId(final Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    @Override
    public void deleteByUserId(final Long userId) {
        redisTemplate.delete(key(userId));
    }

    private static String key(final Long userId) {
        return KEY_PREFIX + userId;
    }
}
