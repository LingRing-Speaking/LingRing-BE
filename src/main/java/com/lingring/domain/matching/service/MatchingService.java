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
    }

    public MatchingStatusResponse getStatus(final Long userId) {
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
            expireConfirmation(confirmation, now);
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
                    .ifPresent(confirmation -> expireConfirmation(confirmation, now));
            return;
        }
        if (outcome == AcceptOutcome.ACCEPTED_WAITING) {
            return;
        }
        // MATCHED
        persistCallForMatched(userId, now);
    }

    public void declineMatch(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        final MatchConfirmation confirmation = matchConfirmationRepository.findByUser(userId)
                .orElseThrow(() -> new MatchConfirmationNotFoundException(userId));
        expireConfirmation(confirmation, now);
    }

    public void expireOverdueConfirmations() {
        final LocalDateTime now = dateTimeProvider.now();
        final List<MatchConfirmation> expired = matchConfirmationRepository.findAllExpired(now);
        for (final MatchConfirmation c : expired) {
            expireConfirmation(c, now);
        }
    }

    private void persistCallForMatched(final Long userId, final LocalDateTime now) {
        final MatchingResult promoted = matchingQueueRepository.findResult(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Result missing after MATCHED promotion for userId=" + userId));
        callRepository.save(Call.start(userId, promoted.partnerId(), promoted.roomId(), now));
    }

    private void expireConfirmation(final MatchConfirmation confirmation, final LocalDateTime now) {
        pairCooldownRepository.put(confirmation.pairKey(), Duration.ofMinutes(matchingProperties.cooldownMinutes()));
        matchConfirmationRepository.delete(confirmation.pairKey());
        matchingQueueRepository.enqueue(confirmation.userAId(), now);
        matchingQueueRepository.enqueue(confirmation.userBId(), now);
    }
}
