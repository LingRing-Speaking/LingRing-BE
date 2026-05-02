package com.lingring.domain.signaling.facade;

import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.service.CallService;
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

    private final CallService callService;
    private final SignalingMessageRouter messageRouter;
    private final SignalingReadyCoordinator readyCoordinator;

    public void dispatch(final Long senderId, final UUID roomId, final SignalingMessage message) {
        final Call call = callService.getByRoomId(roomId);

        final SignalingMessageType type = message.type();
        if (type == SignalingMessageType.JOIN) {
            readyCoordinator.recordJoinAndAnnounceIfReady(call, senderId);
            return;
        }
        if (isForwardable(type)) {
            messageRouter.forwardToCounterpart(call, senderId, message);
            return;
        }
        if (type == SignalingMessageType.HANGUP) {
            messageRouter.forwardToCounterpart(call, senderId, message);
            callService.endCall(roomId);
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
        final Optional<Call> callOpt = callService.findByRoomId(roomId);
        if (callOpt.isEmpty()) {
            return;
        }
        final Call call = callOpt.get();
        if (!call.involves(userId)) {
            return;
        }
        if (!call.isActive()) {
            return;
        }
        messageRouter.publishHangup(call, userId);
        callService.endCall(roomId);
        readyCoordinator.cleanupRoom(roomId);
    }

    private static boolean isForwardable(final SignalingMessageType type) {
        return type == SignalingMessageType.OFFER
                || type == SignalingMessageType.ANSWER
                || type == SignalingMessageType.ICE_CANDIDATE;
    }
}
