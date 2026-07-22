package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.CallEndReason;
import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final DateTimeProvider dateTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Optional<Call> findByRoomId(final UUID roomId) {
        return callRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public Call getByRoomId(final UUID roomId) {
        return callRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallNotFoundException(roomId));
    }

    @Transactional
    public void endCall(final UUID roomId, final CallEndReason reason) {
        final Call call = callRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallNotFoundException(roomId));
        if (!call.isActive()) {
            return;
        }
        final LocalDateTime endedAt = dateTimeProvider.now();
        call.end(endedAt);
        eventPublisher.publishEvent(new CallEndedEvent(
                call.getUserAId(),
                call.getUserBId(),
                call.getStartedAt(),
                endedAt
        ));
        publishCallEnded(call.getUserAId(), call, reason, endedAt);
        publishCallEnded(call.getUserBId(), call, reason, endedAt);
    }

    // 탈퇴 익명화로 user_id가 null인 참여자는 수집 대상에서 제외
    private void publishCallEnded(final Long userId, final Call call,
            final CallEndReason reason, final LocalDateTime endedAt) {
        if (userId == null) {
            return;
        }
        eventPublisher.publishEvent(UserActionEvent.callEnded(
                userId, reason.name(), call.getDurationSec(), call.getRoomId(), endedAt));
    }
}
