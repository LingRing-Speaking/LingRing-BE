package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class MatchingPolicies {

    private final List<MatchingPolicy> policies;

    public MatchingPolicies(final List<MatchingPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    public MatchingFilters filtersFor(final MatchingCandidate self) {
        final List<Predicate<MatchingCandidate>> filters = new ArrayList<>(policies.size());
        for (final MatchingPolicy policy : policies) {
            filters.add(policy.filterFor(self));
        }
        return new MatchingFilters(filters);
    }
}
