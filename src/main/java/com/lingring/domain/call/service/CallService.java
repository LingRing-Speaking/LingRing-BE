package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.call.exception.CallNotFoundException;
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
    public void endCall(final UUID roomId) {
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
    }
}
