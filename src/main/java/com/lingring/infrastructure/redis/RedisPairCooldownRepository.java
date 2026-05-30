package com.lingring.infrastructure.redis;

import com.lingring.domain.matching.dao.PairCooldownRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisPairCooldownRepository implements PairCooldownRepository {

    private static final String KEY_PREFIX = "matching:cooldown:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void put(final String pairKey, final Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + pairKey, "1", ttl);
    }

    @Override
    public boolean contains(final String pairKey) {
        final Boolean exists = redisTemplate.hasKey(KEY_PREFIX + pairKey);
        return Boolean.TRUE.equals(exists);
    }
}
