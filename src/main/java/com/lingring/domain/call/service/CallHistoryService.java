package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallHistoryRepository;
import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.call.exception.CallHistoryNotFoundException;
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
public class CallHistoryService {

    private final CallHistoryRepository callHistoryRepository;
    private final DateTimeProvider dateTimeProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Optional<CallHistory> findByRoomId(final UUID roomId) {
        return callHistoryRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public CallHistory getByRoomId(final UUID roomId) {
        return callHistoryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallHistoryNotFoundException(roomId));
    }

    @Transactional
    public void endCall(final UUID roomId) {
        final CallHistory callHistory = callHistoryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallHistoryNotFoundException(roomId));
        if (!callHistory.isActive()) {
            return;
        }
        final LocalDateTime endedAt = dateTimeProvider.now();
        callHistory.end(endedAt);
        eventPublisher.publishEvent(new CallEndedEvent(
                callHistory.getUserAId(),
                callHistory.getUserBId(),
                callHistory.getStartedAt(),
                endedAt
        ));
    }
}
