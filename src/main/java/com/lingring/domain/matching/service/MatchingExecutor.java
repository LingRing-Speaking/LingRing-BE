package com.lingring.domain.matching.service;

import com.lingring.global.config.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingFailReason;
import com.lingring.domain.matching.domain.MatchingQueue;
import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.global.util.DateTimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingExecutor {

    private static final int MATCHING_SIZE = 2;

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchConfirmationRepository matchConfirmationRepository;
    private final MatchingPolicies matchingPolicies;
    private final DateTimeProvider dateTimeProvider;
    private final MatchingProperties matchingProperties;
    private final ApplicationEventPublisher eventPublisher;

    public void executeRound() {
        final List<MatchingCandidate> candidates = matchingQueueRepository.findAllOrderByEnqueuedAt();
        final List<MatchingCandidate> live = reapDeadCandidates(candidates);
        if (live.size() < MATCHING_SIZE) {
            return;
        }
        final MatchingQueue queue = new MatchingQueue(live);
        final Set<Long> consumed = new HashSet<>();

        for (final MatchingCandidate self : live) {
            checkAndExecute(self, consumed, queue);
        }
    }

    // 이탈(alive 키 만료) 후보는 큐에서 제거하고, 살아있는 후보만 매칭 대상으로 남긴다.
    private List<MatchingCandidate> reapDeadCandidates(final List<MatchingCandidate> candidates) {
        final List<MatchingCandidate> live = new ArrayList<>(candidates.size());
        for (final MatchingCandidate candidate : candidates) {
            if (matchingQueueRepository.isAlive(candidate.userId())) {
                live.add(candidate);
                continue;
            }
            matchingQueueRepository.remove(candidate.userId());
            publishConnectionLost(candidate);
        }
        return live;
    }

    private void publishConnectionLost(final MatchingCandidate candidate) {
        final LocalDateTime now = dateTimeProvider.now();
        eventPublisher.publishEvent(UserActionEvent.matchingFailed(
                candidate.userId(),
                MatchingFailReason.CONNECTION_LOST.name(),
                Duration.between(candidate.enqueuedAt(), now).toMillis(),
                false,
                now
        ));
    }

    private void checkAndExecute(final MatchingCandidate self, final Set<Long> consumed, final MatchingQueue queue) {
        if (consumed.contains(self.userId())) {
            return;
        }
        final Optional<MatchingCandidate> partner = queue.findPartnerFor(self, matchingPolicies, consumed);
        if (partner.isEmpty()) {
            return;
        }
        final MatchingCandidate partnerCandidate = partner.get();
        final Long partnerId = partnerCandidate.userId();
        final UUID roomId = UUID.randomUUID();
        final LocalDateTime now = dateTimeProvider.now();
        final LocalDateTime deadline = now.plusSeconds(matchingProperties.confirmDeadlineSeconds());
        final boolean committed = matchConfirmationRepository.commit(
                self.userId(), partnerId, roomId, deadline, self.enqueuedAt(), partnerCandidate.enqueuedAt());
        if (!committed) {
            return;
        }
        consumed.add(self.userId());
        consumed.add(partnerId);
    }
}
