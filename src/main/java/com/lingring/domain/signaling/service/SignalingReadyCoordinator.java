package com.lingring.domain.signaling.service;

import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.infrastructure.redis.RedisSignalingPublisher;
import com.lingring.infrastructure.redis.SignalingChannels;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class SignalingReadyCoordinator {

    private static final Duration JOINED_SET_TTL = Duration.ofHours(1);
    private static final long EXPECTED_PARTICIPANTS = 2L;

    private final RedisSignalingPublisher signalingPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void recordJoinAndAnnounceIfReady(final Match match, final Long senderId) {
        final String joinedKey = SignalingChannels.joinedSetKey(match.getRoomId());
        redisTemplate.opsForSet().add(joinedKey, senderId.toString());
        redisTemplate.expire(joinedKey, JOINED_SET_TTL);

        final Long size = redisTemplate.opsForSet().size(joinedKey);
        if (size == null || size < EXPECTED_PARTICIPANTS) {
            return;
        }
        if (!acquireReadyLock(match.getRoomId())) {
            return;
        }
        publishReady(match);
    }

    public void cleanupRoom(final UUID roomId) {
        redisTemplate.delete(SignalingChannels.joinedSetKey(roomId));
        redisTemplate.delete(SignalingChannels.readyLockKey(roomId));
    }

    private boolean acquireReadyLock(final UUID roomId) {
        final String lockKey = SignalingChannels.readyLockKey(roomId);
        final Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", JOINED_SET_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void publishReady(final Match match) {
        final ObjectNode payload = objectMapper.createObjectNode()
                .put("callerUserId", match.callerUserId())
                .put("calleeUserId", match.calleeUserId());
        signalingPublisher.publish(match.getRoomId(), new SignalingMessage(
                SignalingMessageType.READY, null, match.callerUserId(), payload));
        signalingPublisher.publish(match.getRoomId(), new SignalingMessage(
                SignalingMessageType.READY, null, match.calleeUserId(), payload));
    }
}
