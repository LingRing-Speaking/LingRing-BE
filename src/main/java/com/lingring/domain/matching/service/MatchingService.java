package com.lingring.domain.matching.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.matching.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchConfirmationRepository.AcceptOutcome;
import com.lingring.domain.matching.dao.MatchConfirmationRepository.AcceptResult;
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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public MatchingStatusResponse getStatus(final Long userId) {
        final Optional<MatchingResult> result = matchingQueueRepository.findResult(userId);
        if (result.isPresent()) {
            final MatchingResult matched = result.get();
            return MatchingStatusResponse.matched(matched.partnerId(), matched.roomId());
        }
        final Optional<MatchConfirmation> confirmation = matchConfirmationRepository.findByUser(userId);
        if (confirmation.isPresent()) {
            final MatchConfirmation c = confirmation.get();
            final LocalDateTime now = dateTimeProvider.now();
            if (c.isExpired(now)) {
                expireConfirmation(c, now);
                return MatchingStatusResponse.waiting();
            }
            return MatchingStatusResponse.awaitingConfirm(c.partnerOf(userId), c.deadline());
        }
        if (matchingQueueRepository.contains(userId)) {
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.none();
    }

    public void leaveQueue(final Long userId) {
        matchingQueueRepository.remove(userId);
    }

    @Transactional
    public void acceptMatch(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        final Optional<MatchConfirmation> before = matchConfirmationRepository.findByUser(userId);
        if (before.isEmpty()) {
            throw new MatchConfirmationNotFoundException(userId);
        }
        if (before.get().isExpired(now)) {
            expireConfirmation(before.get(), now);
            return;
        }
        final AcceptResult result = matchConfirmationRepository.accept(userId, now);
        final AcceptOutcome outcome = result.outcome();
        if (outcome == AcceptOutcome.NOT_FOUND) {
            throw new MatchConfirmationNotFoundException(userId);
        }
        if (outcome == AcceptOutcome.EXPIRED) {
            final Optional<MatchConfirmation> stale = matchConfirmationRepository.findByUser(userId);
            stale.ifPresent(c -> expireConfirmation(c, now));
            return;
        }
        if (outcome == AcceptOutcome.ACCEPTED_WAITING) {
            return;
        }
        // MATCHED
        persistCallForMatched(userId, now);
    }

    @Transactional
    public void declineMatch(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        final MatchConfirmation confirmation = matchConfirmationRepository.findByUser(userId)
                .orElseThrow(() -> new MatchConfirmationNotFoundException(userId));
        expireConfirmation(confirmation, now);
    }

    @Transactional
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
        pairCooldownRepository.put(confirmation.pairKey(),
                Duration.ofMinutes(matchingProperties.cooldownMinutes()));
        matchConfirmationRepository.delete(confirmation.pairKey());
        matchingQueueRepository.enqueue(confirmation.userAId(), now);
        matchingQueueRepository.enqueue(confirmation.userBId(), now);
    }
}
