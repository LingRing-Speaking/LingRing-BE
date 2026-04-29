package com.lingring.domain.signaling.service;

import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.signaling.domain.SignalingMessage;
import com.lingring.domain.signaling.domain.SignalingMessageType;
import com.lingring.infrastructure.redis.RedisSignalingPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignalingMessageRouter {

    private final RedisSignalingPublisher signalingPublisher;

    public void forwardToCounterpart(final Match match, final Long senderId, final SignalingMessage message) {
        final Long counterpart = match.counterpartOf(senderId);
        signalingPublisher.publish(match.getRoomId(), message.withRouting(senderId, counterpart));
    }

    public void publishHangup(final Match match, final Long fromUserId) {
        final Long counterpart = match.counterpartOf(fromUserId);
        final SignalingMessage hangup = new SignalingMessage(
                SignalingMessageType.HANGUP, fromUserId, counterpart, null);
        signalingPublisher.publish(match.getRoomId(), hangup);
    }
}
