package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MatchingPolicies {

    private final List<MatchingPolicy> policies;

    public MatchingPolicies(final List<MatchingPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    public List<MatchingCandidate> filterCandidates(
            final MatchingCandidate self,
            final List<MatchingCandidate> candidates
    ) {
        List<MatchingCandidate> result = candidates;
        for (final MatchingPolicy policy : policies) {
            result = policy.filterCandidates(self, result);
        }
        return result;
    }
}
