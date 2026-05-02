package com.lingring.domain.signaling.facade;

import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.call.service.CallHistoryService;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.domain.signaling.service.SignalingMessageRouter;
import com.lingring.domain.signaling.service.SignalingReadyCoordinator;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalingFacade {

    private final CallHistoryService callHistoryService;
    private final SignalingMessageRouter messageRouter;
    private final SignalingReadyCoordinator readyCoordinator;

    public void dispatch(final Long senderId, final UUID roomId, final SignalingMessage message) {
        final CallHistory callHistory = callHistoryService.getByRoomId(roomId);

        final SignalingMessageType type = message.type();
        if (type == SignalingMessageType.JOIN) {
            readyCoordinator.recordJoinAndAnnounceIfReady(callHistory, senderId);
            return;
        }
        if (isForwardable(type)) {
            messageRouter.forwardToCounterpart(callHistory, senderId, message);
            return;
        }
        if (type == SignalingMessageType.HANGUP) {
            messageRouter.forwardToCounterpart(callHistory, senderId, message);
            callHistoryService.endCall(roomId);
            readyCoordinator.cleanupRoom(roomId);
            return;
        }
        log.error("Unhandled signaling message type from sender {}: {}", senderId, type);
    }

    // TODO: RDB는 성공, Redis는 실패한다면?
    public void handleDisconnect(final Long userId, final UUID roomId) {
        if (roomId == null) {
            return;
        }
        final Optional<CallHistory> callHistoryOpt = callHistoryService.findByRoomId(roomId);
        if (callHistoryOpt.isEmpty()) {
            return;
        }
        final CallHistory callHistory = callHistoryOpt.get();
        if (!callHistory.involves(userId)) {
            return;
        }
        if (!callHistory.isActive()) {
            return;
        }
        messageRouter.publishHangup(callHistory, userId);
        callHistoryService.endCall(roomId);
        readyCoordinator.cleanupRoom(roomId);
    }

    private static boolean isForwardable(final SignalingMessageType type) {
        return type == SignalingMessageType.OFFER
                || type == SignalingMessageType.ANSWER
                || type == SignalingMessageType.ICE_CANDIDATE;
    }
}
