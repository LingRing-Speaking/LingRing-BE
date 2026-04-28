package com.lingring.infrastructure.redis;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.global.util.Zones;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisMatchingQueueRepository implements MatchingQueueRepository {

    private static final String QUEUE_KEY = "matching:queue";
    private static final String RESULT_KEY_PREFIX = "matching:result:";
    private static final Duration RESULT_TTL = Duration.ofSeconds(15);
    private static final DefaultRedisScript<Long> COMMIT_MATCH_SCRIPT = loadCommitMatchScript();

    private final StringRedisTemplate redisTemplate;

    private static DefaultRedisScript<Long> loadCommitMatchScript() {
        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/match-commit.lua"));
        script.setResultType(Long.class);
        return script;
    }

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
    public boolean commitMatch(final Long userId, final Long partnerId) {
        final Long result = redisTemplate.execute(
                COMMIT_MATCH_SCRIPT,
                List.of(QUEUE_KEY, resultKey(userId), resultKey(partnerId)),
                userId.toString(),
                partnerId.toString(),
                String.valueOf(RESULT_TTL.toSeconds())
        );
        return isSuccess(result);
    }

    private boolean isSuccess(final Long result) {
        return result != null && result == 1L;
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
        return enqueuedAt.atZone(Zones.SEOUL).toInstant().toEpochMilli();
    }

    private LocalDateTime fromScore(final double score) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli((long) score), Zones.SEOUL);
    }
}
