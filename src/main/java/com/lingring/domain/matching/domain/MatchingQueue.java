package com.lingring.domain.matching.domain;

import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class MatchingQueue {

    private final List<MatchingCandidate> candidates;

    public MatchingQueue(final List<MatchingCandidate> candidates) {
        this.candidates = List.copyOf(candidates);
    }

    public Optional<MatchingCandidate> findPartnerFor(
            final MatchingCandidate self,
            final MatchingPolicies policies,
            final Set<Long> excludedIds
    ) {
        final List<MatchingCandidate> filtered = candidates.stream()
                .filter(candidate -> !candidate.userId().equals(self.userId()))
                .filter(candidate -> !excludedIds.contains(candidate.userId()))
                .toList();
        return policies.filterCandidates(self, filtered).stream().findFirst();
    }
}
