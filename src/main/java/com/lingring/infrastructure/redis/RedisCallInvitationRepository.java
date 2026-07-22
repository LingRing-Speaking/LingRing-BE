package com.lingring.infrastructure.redis;

import com.lingring.domain.matching.dao.CallInvitationRepository;
import com.lingring.domain.matching.dao.dto.InvitationCreateOutcome;
import com.lingring.domain.matching.domain.CallInvitation;
import com.lingring.domain.matching.domain.CallInvitationPollStatus;
import com.lingring.domain.matching.domain.CallInvitationResult;
import com.lingring.global.util.Zones;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisCallInvitationRepository implements CallInvitationRepository {

    private static final String OUT_KEY_PREFIX = "matching:invitation:out:";
    private static final String IN_KEY_PREFIX = "matching:invitation:in:";
    private static final String RESULT_KEY_PREFIX = "matching:invitation:result:";
    private static final String DELIMITER = ":";

    private static final DefaultRedisScript<Long> CREATE_SCRIPT = loadScript("scripts/invitation-create.lua");
    private static final DefaultRedisScript<Long> CANCEL_SCRIPT = loadScript("scripts/invitation-cancel.lua");

    private final StringRedisTemplate redisTemplate;

    private static DefaultRedisScript<Long> loadScript(final String path) {
        final DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public InvitationCreateOutcome create(final CallInvitation invitation, final Duration ttl) {
        final Long result = redisTemplate.execute(
                CREATE_SCRIPT,
                List.of(
                        OUT_KEY_PREFIX + invitation.inviterId(),
                        IN_KEY_PREFIX + invitation.inviteeId(),
                        RESULT_KEY_PREFIX + invitation.inviterId()
                ),
                invitation.inviteeId().toString(),
                encode(invitation),
                String.valueOf(ttl.toMillis())
        );
        if (result != null && result == 1L) {
            return InvitationCreateOutcome.CREATED;
        }
        if (result != null && result == -2L) {
            return InvitationCreateOutcome.INVITEE_BUSY;
        }
        return InvitationCreateOutcome.INVITER_BUSY;
    }

    @Override
    public Optional<CallInvitation> findByInvitee(final Long inviteeId) {
        final String value = redisTemplate.opsForValue().get(IN_KEY_PREFIX + inviteeId);
        return decode(inviteeId, value);
    }

    @Override
    public Optional<CallInvitation> claim(final Long inviteeId) {
        final String value = redisTemplate.opsForValue().getAndDelete(IN_KEY_PREFIX + inviteeId);
        return decode(inviteeId, value);
    }

    @Override
    public boolean existsByInviter(final Long inviterId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(OUT_KEY_PREFIX + inviterId));
    }

    @Override
    public boolean cancelByInviter(final Long inviterId) {
        final Long result = redisTemplate.execute(
                CANCEL_SCRIPT,
                List.of(OUT_KEY_PREFIX + inviterId),
                IN_KEY_PREFIX,
                inviterId.toString()
        );
        return result != null && result == 1L;
    }

    @Override
    public void saveResult(final Long inviterId, final CallInvitationResult result, final Duration ttl) {
        redisTemplate.opsForValue().set(RESULT_KEY_PREFIX + inviterId, encodeResult(result), ttl);
    }

    @Override
    public Optional<CallInvitationResult> findResult(final Long inviterId) {
        final String value = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + inviterId);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(decodeResult(value));
    }

    private String encode(final CallInvitation invitation) {
        return invitation.inviterId() + DELIMITER + invitation.roomId() + DELIMITER + toEpochMillis(invitation.deadline());
    }

    private Optional<CallInvitation> decode(final Long inviteeId, final String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String[] parts = value.split(DELIMITER, 3);
        return Optional.of(new CallInvitation(
                Long.parseLong(parts[0]),
                inviteeId,
                UUID.fromString(parts[1]),
                fromEpochMillis(Long.parseLong(parts[2]))
        ));
    }

    private String encodeResult(final CallInvitationResult result) {
        if (result.status() == CallInvitationPollStatus.DECLINED) {
            return CallInvitationPollStatus.DECLINED.name();
        }
        return CallInvitationPollStatus.ACCEPTED.name() + DELIMITER + result.roomId() + DELIMITER + result.callId();
    }

    private CallInvitationResult decodeResult(final String value) {
        if (CallInvitationPollStatus.DECLINED.name().equals(value)) {
            return CallInvitationResult.declined();
        }
        final String[] parts = value.split(DELIMITER, 3);
        return CallInvitationResult.accepted(UUID.fromString(parts[1]), Long.parseLong(parts[2]));
    }

    private long toEpochMillis(final LocalDateTime time) {
        return time.atZone(Zones.SEOUL).toInstant().toEpochMilli();
    }

    private LocalDateTime fromEpochMillis(final long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), Zones.SEOUL);
    }
}
