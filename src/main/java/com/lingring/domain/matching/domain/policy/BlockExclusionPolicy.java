package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockExclusionPolicy implements MatchingPolicy {

    private final UserBlockRepository userBlockRepository;

    @Override
    public List<MatchingCandidate> filterCandidates(
            final MatchingCandidate self,
            final List<MatchingCandidate> candidates
    ) {
        final Set<Long> excludedIds = new HashSet<>();
        excludedIds.addAll(userBlockRepository.findBlockedUserIdsByUserId(self.userId()));
        excludedIds.addAll(userBlockRepository.findUserIdsByBlockedUserId(self.userId()));
        return candidates.stream()
                .filter(candidate -> !excludedIds.contains(candidate.userId()))
                .toList();
    }
}
