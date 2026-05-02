package com.lingring.domain.signaling.service;

import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
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

    private final SignalingPublisher signalingPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void recordJoinAndAnnounceIfReady(final CallHistory callHistory, final Long senderId) {
        final String joinedKey = SignalingChannels.joinedSetKey(callHistory.getRoomId());
        redisTemplate.opsForSet().add(joinedKey, senderId.toString());
        redisTemplate.expire(joinedKey, JOINED_SET_TTL);

        final Long size = redisTemplate.opsForSet().size(joinedKey);
        if (size == null || size < EXPECTED_PARTICIPANTS) {
            return;
        }
        if (!acquireReadyLock(callHistory.getRoomId())) {
            return;
        }
        publishReady(callHistory);
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

    private void publishReady(final CallHistory callHistory) {
        final ObjectNode payload = objectMapper.createObjectNode()
                .put("callerUserId", callHistory.callerUserId())
                .put("calleeUserId", callHistory.calleeUserId());
        signalingPublisher.publish(callHistory.getRoomId(), new SignalingMessage(
                SignalingMessageType.READY, null, callHistory.callerUserId(), payload));
        signalingPublisher.publish(callHistory.getRoomId(), new SignalingMessage(
                SignalingMessageType.READY, null, callHistory.calleeUserId(), payload));
    }
}
