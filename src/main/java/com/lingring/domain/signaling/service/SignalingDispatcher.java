package com.lingring.domain.signaling.service;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.service.MatchingService;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.domain.signaling.dao.LocalSessionRegistry;
import com.lingring.global.error.ErrorCode;
import com.lingring.infrastructure.redis.RedisSignalingPublisher;
import com.lingring.infrastructure.redis.SignalingChannels;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalingDispatcher {

    private static final Duration JOINED_SET_TTL = Duration.ofHours(1);
    private static final long EXPECTED_PARTICIPANTS = 2L;

    private final MatchRepository matchRepository;
    private final MatchingService matchingService;
    private final LocalSessionRegistry sessionRegistry;
    private final RedisSignalingPublisher signalingPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void dispatch(final Long senderId, final SignalingMessage message) {
        final Optional<Match> matchOpt = matchRepository.findByRoomId(message.roomId());
        if (matchOpt.isEmpty()) {
            sendErrorToSender(senderId, message.roomId(), ErrorCode.MATCH_NOT_FOUND);
            return;
        }
        final Match match = matchOpt.get();
        if (!match.involves(senderId)) {
            sendErrorToSender(senderId, message.roomId(), ErrorCode.MATCH_PARTICIPANT_MISMATCH);
            return;
        }

        final SignalingMessageType type = message.type();
        if (type == SignalingMessageType.JOIN) {
            handleJoin(senderId, match);
            return;
        }
        if (type == SignalingMessageType.OFFER
                || type == SignalingMessageType.ANSWER
                || type == SignalingMessageType.ICE_CANDIDATE) {
            handleForward(senderId, match, message);
            return;
        }
        if (type == SignalingMessageType.HANGUP) {
            handleHangup(senderId, match, message);
            return;
        }
        log.warn("Unhandled signaling message type from sender {}: {}", senderId, type);
    }

    public void deliverLocally(final SignalingMessage message) {
        if (message.toUserId() == null) {
            return;
        }
        sessionRegistry.find(message.toUserId())
                .ifPresent(session -> sendThroughSession(session, message));
    }

    public void handleDisconnect(final Long userId, final UUID roomId) {
        if (roomId == null) {
            return;
        }
        final Optional<Match> matchOpt = matchRepository.findByRoomId(roomId);
        if (matchOpt.isEmpty()) {
            return;
        }
        final Match match = matchOpt.get();
        if (!match.involves(userId)) {
            return;
        }
        final Long counterpart = match.counterpartOf(userId);
        final SignalingMessage hangup = new SignalingMessage(
                SignalingMessageType.HANGUP, roomId, userId, counterpart, null);
        signalingPublisher.publish(hangup);
        matchingService.endMatch(roomId);
        redisTemplate.delete(SignalingChannels.joinedSetKey(roomId));
        redisTemplate.delete(SignalingChannels.readyLockKey(roomId));
    }

    private void handleJoin(final Long senderId, final Match match) {
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

    private boolean acquireReadyLock(final UUID roomId) {
        final String lockKey = SignalingChannels.readyLockKey(roomId);
        final Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", JOINED_SET_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void publishReady(final Match match) {
        final ObjectNode payload = objectMapper.createObjectNode()
                .put("callerUserId", match.callerUserId())
                .put("calleeUserId", match.calleeUserId());
        signalingPublisher.publish(new SignalingMessage(
                SignalingMessageType.READY, match.getRoomId(), null, match.callerUserId(), payload));
        signalingPublisher.publish(new SignalingMessage(
                SignalingMessageType.READY, match.getRoomId(), null, match.calleeUserId(), payload));
    }

    private void handleForward(final Long senderId, final Match match, final SignalingMessage message) {
        final Long counterpart = match.counterpartOf(senderId);
        signalingPublisher.publish(message.withRouting(senderId, counterpart));
    }

    private void handleHangup(final Long senderId, final Match match, final SignalingMessage message) {
        final Long counterpart = match.counterpartOf(senderId);
        signalingPublisher.publish(message.withRouting(senderId, counterpart));
        matchingService.endMatch(match.getRoomId());
        redisTemplate.delete(SignalingChannels.joinedSetKey(match.getRoomId()));
        redisTemplate.delete(SignalingChannels.readyLockKey(match.getRoomId()));
    }

    private void sendErrorToSender(final Long senderId, final UUID roomId, final ErrorCode errorCode) {
        final ObjectNode payload = objectMapper.createObjectNode()
                .put("code", errorCode.name())
                .put("message", errorCode.getMessage());
        final SignalingMessage error = new SignalingMessage(
                SignalingMessageType.ERROR, roomId, null, senderId, payload);
        sessionRegistry.find(senderId).ifPresent(session -> sendThroughSession(session, error));
    }

    private void sendThroughSession(final WebSocketSession session, final SignalingMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (final Exception e) {
            log.warn("Failed to send signaling message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}
