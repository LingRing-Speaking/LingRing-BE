package com.lingring.infrastructure.redis;

import com.lingring.domain.presence.dao.PresenceRepository;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisPresenceRepository implements PresenceRepository {

    private static final String KEY_PREFIX = "presence:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void markOnline(final Long userId, final Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", ttl);
    }

    @Override
    public void markOffline(final Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    @Override
    public Set<Long> findOnlineUserIds(final Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        final List<Long> orderedUserIds = List.copyOf(userIds);
        final List<String> keys = orderedUserIds.stream()
                .map(userId -> KEY_PREFIX + userId)
                .toList();
        final List<String> values = redisTemplate.opsForValue().multiGet(keys);
        final Set<Long> onlineUserIds = new HashSet<>();
        for (int i = 0; i < orderedUserIds.size(); i++) {
            if (values.get(i) != null) {
                onlineUserIds.add(orderedUserIds.get(i));
            }
        }
        return onlineUserIds;
    }
}
