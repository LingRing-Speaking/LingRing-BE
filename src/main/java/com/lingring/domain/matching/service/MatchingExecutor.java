package com.lingring.domain.matching.service;

import com.lingring.domain.matching.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingQueue;
import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingExecutor {

    private static final int MATCHING_SIZE = 2;

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchConfirmationRepository matchConfirmationRepository;
    private final MatchingPolicies matchingPolicies;
    private final RoomIdGenerator roomIdGenerator;
    private final DateTimeProvider dateTimeProvider;
    private final MatchingProperties matchingProperties;

    public void executeRound() {
        final List<MatchingCandidate> candidates = matchingQueueRepository.findAllOrderByEnqueuedAt();
        if (candidates.size() < MATCHING_SIZE) {
            return;
        }
        final MatchingQueue queue = new MatchingQueue(candidates);
        final Set<Long> consumed = new HashSet<>();

        for (final MatchingCandidate self : candidates) {
            checkAndExecute(self, consumed, queue);
        }
    }

    private void checkAndExecute(final MatchingCandidate self, final Set<Long> consumed, final MatchingQueue queue) {
        if (consumed.contains(self.userId())) {
            return;
        }
        final Optional<MatchingCandidate> partner = queue.findPartnerFor(self, matchingPolicies, consumed);
        if (partner.isEmpty()) {
            return;
        }
        final Long partnerId = partner.get().userId();
        final UUID roomId = roomIdGenerator.generate();
        final LocalDateTime now = dateTimeProvider.now();
        final LocalDateTime deadline = now.plusSeconds(matchingProperties.confirmDeadlineSeconds());
        final boolean committed = matchConfirmationRepository.commit(self.userId(), partnerId, roomId, deadline);
        if (!committed) {
            return;
        }
        consumed.add(self.userId());
        consumed.add(partnerId);
    }
}
