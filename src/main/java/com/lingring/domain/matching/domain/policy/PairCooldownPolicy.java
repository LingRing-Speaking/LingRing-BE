package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingCandidate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PairCooldownPolicy implements MatchingPolicy {

    private final PairCooldownRepository pairCooldownRepository;

    @Override
    public List<MatchingCandidate> filterCandidates(
            final MatchingCandidate self,
            final List<MatchingCandidate> candidates
    ) {
        return candidates.stream()
                .filter(candidate -> !pairCooldownRepository.contains(
                        MatchConfirmation.pairKeyOf(self.userId(), candidate.userId())))
                .toList();
    }
}
