package com.lingring.infrastructure.redis;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisMatchingQueueRepository implements MatchingQueueRepository {

    private static final String QUEUE_KEY = "matching:queue";
    private static final String RESULT_KEY_PREFIX = "matching:result:";
    private static final Duration RESULT_TTL = Duration.ofSeconds(60);
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;

    @Override
    public void enqueue(final Long userId, final LocalDateTime enqueuedAt) {
        redisTemplate.opsForZSet().add(QUEUE_KEY, userId.toString(), toScore(enqueuedAt));
    }

    @Override
    public void remove(final Long userId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, userId.toString());
    }

    @Override
    public boolean contains(final Long userId) {
        return redisTemplate.opsForZSet().score(QUEUE_KEY, userId.toString()) != null;
    }

    @Override
    public List<MatchingCandidate> findAllOrderByEnqueuedAt() {
        final Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(QUEUE_KEY, 0, -1);
        if (tuples == null) {
            return List.of();
        }
        final List<MatchingCandidate> candidates = new ArrayList<>(tuples.size());
        for (final ZSetOperations.TypedTuple<String> tuple : tuples) {
            final String value = tuple.getValue();
            final Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }
            candidates.add(new MatchingCandidate(Long.parseLong(value), fromScore(score)));
        }
        return candidates;
    }

    @Override
    public void saveResult(final Long userId, final Long partnerId) {
        redisTemplate.opsForValue().set(resultKey(userId), partnerId.toString(), RESULT_TTL);
    }

    @Override
    public void commitMatch(final Long userId, final Long partnerId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, userId.toString(), partnerId.toString());
        redisTemplate.opsForValue().set(resultKey(userId), partnerId.toString(), RESULT_TTL);
        redisTemplate.opsForValue().set(resultKey(partnerId), userId.toString(), RESULT_TTL);
    }

    @Override
    public Optional<Long> findResult(final Long userId) {
        final String value = redisTemplate.opsForValue().get(resultKey(userId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(value));
    }

    @Override
    public void clearResult(final Long userId) {
        redisTemplate.delete(resultKey(userId));
    }

    private String resultKey(final Long userId) {
        return RESULT_KEY_PREFIX + userId;
    }

    private double toScore(final LocalDateTime enqueuedAt) {
        return enqueuedAt.atZone(SEOUL_ZONE).toInstant().toEpochMilli();
    }

    private LocalDateTime fromScore(final double score) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli((long) score), SEOUL_ZONE);
    }
}
