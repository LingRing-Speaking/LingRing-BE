package com.lingring.domain.matching.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingCandidate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PairCooldownPolicyTest {

    private final PairCooldownRepository repository = Mockito.mock(PairCooldownRepository.class);
    private final PairCooldownPolicy policy = new PairCooldownPolicy(repository);

    @Test
    @DisplayName("cooldown 중인 페어는 후보에서 제외된다")
    void filter_excludesPairsInCooldown() {
        // given
        final LocalDateTime enqueuedAt = LocalDateTime.of(2026, 5, 12, 12, 0, 0);
        final MatchingCandidate self = new MatchingCandidate(1L, enqueuedAt);
        final MatchingCandidate cooled = new MatchingCandidate(2L, enqueuedAt);
        final MatchingCandidate fresh = new MatchingCandidate(3L, enqueuedAt);
        given(repository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).willReturn(true);
        given(repository.contains(MatchConfirmation.pairKeyOf(1L, 3L))).willReturn(false);

        // when
        final List<MatchingCandidate> result = policy.filterCandidates(self, List.of(cooled, fresh));

        // then
        assertThat(result).extracting(MatchingCandidate::userId).containsExactly(3L);
    }
}
