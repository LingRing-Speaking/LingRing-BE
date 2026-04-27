package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.List;

public interface MatchingPolicy {

    List<MatchingCandidate> filterCandidates(MatchingCandidate self, List<MatchingCandidate> candidates);
}
