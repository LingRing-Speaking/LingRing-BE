package com.lingring.domain.matching.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.global.config.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.dto.AcceptOutcome;
import com.lingring.domain.matching.dao.dto.AcceptResult;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.exception.MatchConfirmationNotFoundException;
import com.lingring.global.util.DateTimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchConfirmationRepository matchConfirmationRepository;
    private final PairCooldownRepository pairCooldownRepository;
    private final CallRepository callRepository;
    private final DateTimeProvider dateTimeProvider;
    private final MatchingProperties matchingProperties;

    public void enterQueue(final Long userId) {
        matchingQueueRepository.clearResult(userId);
        matchingQueueRepository.enqueue(userId, dateTimeProvider.now());
        matchingQueueRepository.markAlive(userId, matchingProperties.aliveTtl());
    }

    public MatchingStatusResponse getStatus(final Long userId) {
        matchingQueueRepository.markAlive(userId, matchingProperties.aliveTtl());
        final Optional<MatchingResult> result = matchingQueueRepository.findResult(userId);
        if (result.isPresent()) {
            return matchedResponse(result.get());
        }
        final Optional<MatchConfirmation> confirmation = matchConfirmationRepository.findByUser(userId);
        if (confirmation.isPresent()) {
            return confirmationResponse(userId, confirmation.get());
        }
        return queuedResponse(userId);
    }

    private MatchingStatusResponse matchedResponse(final MatchingResult result) {
        final Long callId = callRepository.findByRoomId(result.roomId())
                .map(Call::getId)
                .orElse(null);
        return MatchingStatusResponse.matched(result.partnerId(), result.roomId(), callId);
    }

    private MatchingStatusResponse confirmationResponse(final Long userId, final MatchConfirmation confirmation) {
        final LocalDateTime now = dateTimeProvider.now();
        if (confirmation.isExpired(now)) {
            cancelByDeadline(confirmation, now);
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.awaitingConfirm(confirmation.partnerOf(userId), confirmation.deadline());
    }

    private MatchingStatusResponse queuedResponse(final Long userId) {
        if (matchingQueueRepository.contains(userId)) {
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.none();
    }

    public void leaveQueue(final Long userId) {
        matchingQueueRepository.remove(userId);
    }

    public void acceptMatch(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        final AcceptResult result = matchConfirmationRepository.accept(userId, now);
        final AcceptOutcome outcome = result.outcome();
        if (outcome == AcceptOutcome.NOT_FOUND) {
            throw new MatchConfirmationNotFoundException(userId);
        }
        if (outcome == AcceptOutcome.EXPIRED) {
            matchConfirmationRepository.findByUser(userId)
                    .ifPresent(confirmation -> cancelByDeadline(confirmation, now));
            return;
        }
        if (outcome == AcceptOutcome.ACCEPTED_WAITING) {
            return;
        }
        // MATCHED
        persistCallForMatched(userId, now);
    }

    public void declineMatch(final Long userId) {
        final MatchConfirmation confirmation = matchConfirmationRepository.findByUser(userId)
                .orElseThrow(() -> new MatchConfirmationNotFoundException(userId));
        cancelByDecline(confirmation);
    }

    public void expireOverdueConfirmations() {
        final LocalDateTime now = dateTimeProvider.now();
        final List<MatchConfirmation> expired = matchConfirmationRepository.findAllExpired(now);
        for (final MatchConfirmation c : expired) {
            cancelByDeadline(c, now);
        }
    }

    private void persistCallForMatched(final Long userId, final LocalDateTime now) {
        final MatchingResult promoted = matchingQueueRepository.findResult(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Result missing after MATCHED promotion for userId=" + userId));
        callRepository.save(Call.start(userId, promoted.partnerId(), promoted.roomId(), now));
    }

    // 마감 만료: 수락한 쪽은 원래 대기순서 보존, 무응답은 now로 강등
    private void cancelByDeadline(final MatchConfirmation confirmation, final LocalDateTime now) {
        removeWithCooldown(confirmation);
        requeueAfterDeadline(confirmation.userAId(), confirmation.userAAccepted(), confirmation.userAEnqueuedAt(), now);
        requeueAfterDeadline(confirmation.userBId(), confirmation.userBAccepted(), confirmation.userBEnqueuedAt(), now);
    }

    // 거절: 양쪽 모두 원래 대기순서 보존 (거절자=권리 행사, 상대=마감 전 중단된 피해자)
    private void cancelByDecline(final MatchConfirmation confirmation) {
        removeWithCooldown(confirmation);
        matchingQueueRepository.enqueue(confirmation.userAId(), confirmation.userAEnqueuedAt());
        matchingQueueRepository.enqueue(confirmation.userBId(), confirmation.userBEnqueuedAt());
    }

    private void requeueAfterDeadline(final Long userId, final boolean accepted,
            final LocalDateTime originalEnqueuedAt, final LocalDateTime now) {
        if (accepted) {
            matchingQueueRepository.enqueue(userId, originalEnqueuedAt);
            return;
        }
        matchingQueueRepository.enqueue(userId, now);
    }

    private void removeWithCooldown(final MatchConfirmation confirmation) {
        pairCooldownRepository.put(confirmation.pairKey(), Duration.ofMinutes(matchingProperties.cooldownMinutes()));
        matchConfirmationRepository.delete(confirmation.pairKey());
    }
}
