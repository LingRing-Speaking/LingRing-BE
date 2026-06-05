package com.lingring.infrastructure.redis;

import com.lingring.global.config.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.dto.AcceptOutcome;
import com.lingring.domain.matching.dao.dto.AcceptResult;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.global.util.Zones;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisMatchConfirmationRepository implements MatchConfirmationRepository {

    private static final String QUEUE_KEY = "matching:queue";
    private static final String CONFIRM_KEY_PREFIX = "matching:confirm:";
    private static final String CONFIRM_USER_KEY_PREFIX = "matching:confirm:user:";
    private static final String RESULT_KEY_PREFIX = "matching:result:";
    private static final String RESULT_DELIMITER = ":";
    private static final Duration RESULT_TTL = Duration.ofSeconds(15);
    private static final Duration EXPIRY_GRACE = Duration.ofSeconds(30);

    private static final DefaultRedisScript<Long> COMMIT_SCRIPT = loadCommitScript();
    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> ACCEPT_SCRIPT = loadAcceptScript();

    private final StringRedisTemplate redisTemplate;
    private final MatchingProperties matchingProperties;

    private static DefaultRedisScript<Long> loadCommitScript() {
        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/confirm-commit.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> loadAcceptScript() {
        final DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/confirm-accept.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Override
    public boolean commit(final Long userAId, final Long userBId, final UUID roomId, final LocalDateTime deadline) {
        final Long lo = Math.min(userAId, userBId);
        final Long hi = Math.max(userAId, userBId);
        final String pairKey = lo + ":" + hi;
        final long ttlSeconds = matchingProperties.confirmDeadlineSeconds() + EXPIRY_GRACE.toSeconds();
        final Long result = redisTemplate.execute(
                COMMIT_SCRIPT,
                List.of(
                        QUEUE_KEY,
                        CONFIRM_KEY_PREFIX + pairKey,
                        CONFIRM_USER_KEY_PREFIX + lo,
                        CONFIRM_USER_KEY_PREFIX + hi
                ),
                lo.toString(), hi.toString(), roomId.toString(),
                String.valueOf(toEpochMillis(deadline)), pairKey, String.valueOf(ttlSeconds)
        );
        return result != null && result == 1L;
    }

    @Override
    public Optional<MatchConfirmation> findByUser(final Long userId) {
        final String pairKey = redisTemplate.opsForValue().get(CONFIRM_USER_KEY_PREFIX + userId);
        if (pairKey == null) {
            return Optional.empty();
        }
        return readHash(pairKey);
    }

    @Override
    public AcceptResult accept(final Long userId, final LocalDateTime now) {
        @SuppressWarnings("rawtypes")
        final List result = redisTemplate.execute(
                ACCEPT_SCRIPT,
                List.of(CONFIRM_USER_KEY_PREFIX + userId),
                userId.toString(), String.valueOf(toEpochMillis(now)),
                RESULT_KEY_PREFIX, String.valueOf(RESULT_TTL.toSeconds()), RESULT_DELIMITER
        );
        if (result == null || result.isEmpty()) {
            return new AcceptResult(AcceptOutcome.NOT_FOUND, null);
        }
        final long code = Long.parseLong(result.get(0).toString());
        if (code == -1L) {
            return new AcceptResult(AcceptOutcome.NOT_FOUND, null);
        }
        if (code == -2L) {
            return new AcceptResult(AcceptOutcome.EXPIRED, null);
        }
        if (code == 0L) {
            final Optional<MatchConfirmation> after = findByUser(userId);
            return new AcceptResult(AcceptOutcome.ACCEPTED_WAITING, after.orElse(null));
        }
        return new AcceptResult(AcceptOutcome.MATCHED, null);
    }

    @Override
    public void delete(final String pairKey) {
        final Optional<MatchConfirmation> confirmation = readHash(pairKey);
        redisTemplate.delete(CONFIRM_KEY_PREFIX + pairKey);
        confirmation.ifPresent(c -> {
            redisTemplate.delete(CONFIRM_USER_KEY_PREFIX + c.userAId());
            redisTemplate.delete(CONFIRM_USER_KEY_PREFIX + c.userBId());
        });
    }

    @Override
    public List<MatchConfirmation> findAllExpired(final LocalDateTime now) {
        final Set<String> keys = redisTemplate.keys(CONFIRM_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        final List<MatchConfirmation> expired = new ArrayList<>();
        for (final String key : keys) {
            if (key.startsWith(CONFIRM_USER_KEY_PREFIX)) {
                continue;
            }
            final Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                continue;
            }
            final MatchConfirmation c = toConfirmation(entries);
            if (c.isExpired(now)) {
                expired.add(c);
            }
        }
        return expired;
    }

    private Optional<MatchConfirmation> readHash(final String pairKey) {
        final Map<Object, Object> entries = redisTemplate.opsForHash().entries(CONFIRM_KEY_PREFIX + pairKey);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toConfirmation(entries));
    }

    private MatchConfirmation toConfirmation(final Map<Object, Object> entries) {
        final Long userAId = Long.parseLong(entries.get("userAId").toString());
        final Long userBId = Long.parseLong(entries.get("userBId").toString());
        final boolean aOk = "1".equals(entries.get("userAAccepted").toString());
        final boolean bOk = "1".equals(entries.get("userBAccepted").toString());
        final UUID roomId = UUID.fromString(entries.get("roomId").toString());
        final long deadlineMillis = Long.parseLong(entries.get("deadline").toString());
        return new MatchConfirmation(userAId, userBId, aOk, bOk, roomId, fromEpochMillis(deadlineMillis));
    }

    private long toEpochMillis(final LocalDateTime time) {
        return time.atZone(Zones.SEOUL).toInstant().toEpochMilli();
    }

    private LocalDateTime fromEpochMillis(final long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), Zones.SEOUL);
    }
}
