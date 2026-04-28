package com.lingring.domain.matching.service;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingQueue;
import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingExecutor {

    private static final int MATCHING_SIZE = 2;

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchingPolicies matchingPolicies;

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
        final boolean committed = matchingQueueRepository.commitMatch(self.userId(), partnerId);
        if (!committed) {
            return;
        }
        consumed.add(self.userId());
        consumed.add(partnerId);
    }
}
