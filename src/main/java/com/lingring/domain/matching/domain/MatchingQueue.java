package com.lingring.domain.matching.domain;

import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import java.util.List;
import java.util.Optional;

public final class MatchingQueue {

    private final List<MatchingCandidate> candidates;

    public MatchingQueue(final List<MatchingCandidate> candidates) {
        this.candidates = List.copyOf(candidates);
    }

    public Optional<MatchingCandidate> findPartnerFor(
            final MatchingCandidate self,
            final MatchingPolicies policies
    ) {
        final List<MatchingCandidate> excludingSelf = candidates.stream()
                .filter(candidate -> !candidate.userId().equals(self.userId()))
                .toList();
        return policies.filterCandidates(self, excludingSelf).stream().findFirst();
    }
}
