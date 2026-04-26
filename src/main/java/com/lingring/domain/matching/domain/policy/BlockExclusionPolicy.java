package com.lingring.domain.matching.domain.policy;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockExclusionPolicy implements MatchingPolicy {

    private final UserBlockRepository userBlockRepository;

    @Override
    public Predicate<MatchingCandidate> filterFor(final MatchingCandidate self) {
        final Set<Long> excludedIds = new HashSet<>();
        excludedIds.addAll(userBlockRepository.findBlockedUserIdsByUserId(self.userId()));
        excludedIds.addAll(userBlockRepository.findUserIdsByBlockedUserId(self.userId()));
        return candidate -> !excludedIds.contains(candidate.userId());
    }
}
