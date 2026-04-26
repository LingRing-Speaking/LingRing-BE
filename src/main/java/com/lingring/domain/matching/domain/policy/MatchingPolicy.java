package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.function.Predicate;

public interface MatchingPolicy {

    Predicate<MatchingCandidate> filterFor(MatchingCandidate self);
}
