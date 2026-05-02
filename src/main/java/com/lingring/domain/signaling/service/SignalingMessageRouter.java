package com.lingring.domain.signaling.service;

import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignalingMessageRouter {

    private final SignalingPublisher signalingPublisher;

    public void forwardToCounterpart(final CallHistory callHistory, final Long senderId, final SignalingMessage message) {
        final Long counterpart = callHistory.counterpartOf(senderId);
        signalingPublisher.publish(callHistory.getRoomId(), message.withRouting(senderId, counterpart));
    }

    public void publishHangup(final CallHistory callHistory, final Long fromUserId) {
        final Long counterpart = callHistory.counterpartOf(fromUserId);
        final SignalingMessage hangup = new SignalingMessage(
                SignalingMessageType.HANGUP, fromUserId, counterpart, null);
        signalingPublisher.publish(callHistory.getRoomId(), hangup);
    }
}
