package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.List;
import java.util.function.Predicate;

public final class MatchingFilters {

    private final List<Predicate<MatchingCandidate>> filters;

    MatchingFilters(final List<Predicate<MatchingCandidate>> filters) {
        this.filters = List.copyOf(filters);
    }

    public boolean allMatch(final MatchingCandidate candidate) {
        for (final Predicate<MatchingCandidate> filter : filters) {
            if (!filter.test(candidate)) {
                return false;
            }
        }
        return true;
    }
}
