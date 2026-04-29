package com.lingring.domain.signaling.facade;

import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.service.MatchingService;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.domain.signaling.service.SignalingMessageRouter;
import com.lingring.domain.signaling.service.SignalingReadyCoordinator;
import com.lingring.domain.signaling.service.SignalingSessionMessenger;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalingFacade {

    private final MatchingService matchingService;
    private final SignalingMessageRouter messageRouter;
    private final SignalingReadyCoordinator readyCoordinator;
    private final SignalingSessionMessenger sessionMessenger;

    public void dispatch(final Long senderId, final UUID roomId, final SignalingMessage message) {
        final Match match = matchingService.getByRoomId(roomId);

        final SignalingMessageType type = message.type();
        if (type == SignalingMessageType.JOIN) {
            readyCoordinator.recordJoinAndAnnounceIfReady(match, senderId);
            return;
        }
        if (isForwardable(type)) {
            messageRouter.forwardToCounterpart(match, senderId, message);
            return;
        }
        if (type == SignalingMessageType.HANGUP) {
            messageRouter.forwardToCounterpart(match, senderId, message);
            matchingService.endMatch(roomId);
            readyCoordinator.cleanupRoom(roomId);
            return;
        }
        log.warn("Unhandled signaling message type from sender {}: {}", senderId, type);
    }

    public void deliverLocally(final SignalingMessage message) {
        if (message.toUserId() == null) {
            return;
        }
        sessionMessenger.sendToUser(message.toUserId(), message);
    }

    public void handleDisconnect(final Long userId, final UUID roomId) {
        if (roomId == null) {
            return;
        }
        final Optional<Match> matchOpt = matchingService.findByRoomId(roomId);
        if (matchOpt.isEmpty()) {
            return;
        }
        final Match match = matchOpt.get();
        if (!match.involves(userId)) {
            return;
        }
        messageRouter.publishHangup(match, userId);
        matchingService.endMatch(roomId);
        readyCoordinator.cleanupRoom(roomId);
    }

    private static boolean isForwardable(final SignalingMessageType type) {
        return type == SignalingMessageType.OFFER
                || type == SignalingMessageType.ANSWER
                || type == SignalingMessageType.ICE_CANDIDATE;
    }
}
