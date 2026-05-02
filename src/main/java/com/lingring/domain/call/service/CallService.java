package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
    public CallsResponse getCallsByUserId(final Long userId, final int page, final int size) {
        final PageSize pageSize = PageSize.clamp(size);
        final Slice<CallSummaryProjection> slice = callRepository.findEndedSummariesByUserId(userId, PageRequest.of(page, pageSize.value()));
        return CallsResponse.from(slice);
    }

    @Transactional(readOnly = true)
    public Call getByRoomId(final UUID roomId) {
        return callRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CallNotFoundException(roomId));
    }

    @Transactional
    public void anonymizeUser(final Long userId) {
        callRepository.anonymizeUser(userId);
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
