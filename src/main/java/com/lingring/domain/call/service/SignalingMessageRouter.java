package com.lingring.domain.call.service;

import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.SignalingMessage;
import com.lingring.domain.call.domain.SignalingMessageType;
import com.lingring.domain.call.domain.port.SignalingPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignalingMessageRouter {

    private final SignalingPublisher signalingPublisher;

    public void forwardToCounterpart(final Call call, final Long senderId, final SignalingMessage message) {
        final Long counterpart = call.counterpartOf(senderId);
        signalingPublisher.publish(call.getRoomId(), message.withRouting(senderId, counterpart));
    }

    public void publishHangup(final Call call, final Long fromUserId) {
        final Long counterpart = call.counterpartOf(fromUserId);
        final SignalingMessage hangup = new SignalingMessage(
                SignalingMessageType.HANGUP, fromUserId, counterpart, null);
        signalingPublisher.publish(call.getRoomId(), hangup);
    }
}
