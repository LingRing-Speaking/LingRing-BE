package com.lingring.domain.matching.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchingPoliciesTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Nested
    @DisplayName("filterCandidates")
    class FilterCandidates {

        @Test
        @DisplayName("등록된 모든 정책을 순차 적용해 후보를 좁힌다")
        void appliesAllPoliciesSequentially() {
            // given: 정책1은 id<5만 통과, 정책2는 id가 짝수만 통과
            final MatchingPolicies policies = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates.stream()
                            .filter(c -> c.userId() < 5)
                            .toList(),
                    (self, candidates) -> candidates.stream()
                            .filter(c -> c.userId() % 2 == 0)
                            .toList()
            ));
            final List<MatchingCandidate> candidates = List.of(
                    candidate(2L), candidate(3L), candidate(4L), candidate(6L), candidate(7L)
            );

            // when
            final List<MatchingCandidate> result = policies.filterCandidates(candidate(1L), candidates);

            // then: id<5 통과 → {2,3,4}, 짝수 통과 → {2,4}
            assertThat(result).extracting(MatchingCandidate::userId).containsExactly(2L, 4L);
        }

        @Test
        @DisplayName("정책이 하나도 없으면 후보를 그대로 반환한다")
        void returnsAllWhenNoPolicies() {
            // given
            final MatchingPolicies policies = new MatchingPolicies(List.of());
            final List<MatchingCandidate> candidates = List.of(candidate(2L), candidate(3L));

            // when
            final List<MatchingCandidate> result = policies.filterCandidates(candidate(1L), candidates);

            // then
            assertThat(result).extracting(MatchingCandidate::userId).containsExactly(2L, 3L);
        }

        @Test
        @DisplayName("정책 중 하나라도 모두 거부하면 빈 리스트를 반환한다")
        void returnsEmptyWhenAnyPolicyRejectsAll() {
            // given
            final MatchingPolicies policies = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates,
                    (self, candidates) -> List.of()
            ));
            final List<MatchingCandidate> candidates = List.of(candidate(2L), candidate(3L));

            // when
            final List<MatchingCandidate> result = policies.filterCandidates(candidate(1L), candidates);

            // then
            assertThat(result).isEmpty();
        }
    }

    private MatchingCandidate candidate(final Long userId) {
        return new MatchingCandidate(userId, FIXED_TIME);
    }
}
