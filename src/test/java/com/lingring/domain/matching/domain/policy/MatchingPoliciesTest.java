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
    @DisplayName("filtersFor")
    class FiltersFor {

        @Test
        @DisplayName("등록된 모든 정책이 통과되어야 후보가 매칭 가능하다")
        void allMatch_whenEveryPolicyAccepts_returnsTrue() {
            // given
            final MatchingPolicies policies = new MatchingPolicies(List.of(
                    self -> candidate -> true,
                    self -> candidate -> true
            ));
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final MatchingFilters filters = policies.filtersFor(self);

            // then
            assertThat(filters.allMatch(other)).isTrue();
        }

        @Test
        @DisplayName("정책 중 하나라도 거부하면 후보가 매칭 불가능하다")
        void allMatch_whenAnyPolicyRejects_returnsFalse() {
            // given
            final MatchingPolicies policies = new MatchingPolicies(List.of(
                    self -> candidate -> true,
                    self -> candidate -> false
            ));
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final MatchingFilters filters = policies.filtersFor(self);

            // then
            assertThat(filters.allMatch(other)).isFalse();
        }

        @Test
        @DisplayName("정책이 없으면 항상 매칭 가능하다 (vacuous truth)")
        void allMatch_whenNoPolicies_returnsTrue() {
            // given
            final MatchingPolicies policies = new MatchingPolicies(List.of());
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final MatchingFilters filters = policies.filtersFor(self);

            // then
            assertThat(filters.allMatch(other)).isTrue();
        }

        @Test
        @DisplayName("filterFor는 self당 1회만 호출되고, 그 결과 Predicate가 후보별로 재사용된다")
        void filterFor_isCalledOncePerSelf_andReusedAcrossCandidates() {
            // given
            final int[] filterForCallCount = {0};
            final MatchingPolicies policies = new MatchingPolicies(List.of(
                    self -> {
                        filterForCallCount[0]++;
                        return candidate -> true;
                    }
            ));
            final MatchingCandidate self = candidate(1L);

            // when: filtersFor 한 번 호출 후 여러 후보 검사
            final MatchingFilters filters = policies.filtersFor(self);
            filters.allMatch(candidate(2L));
            filters.allMatch(candidate(3L));
            filters.allMatch(candidate(4L));

            // then
            assertThat(filterForCallCount[0]).isEqualTo(1);
        }
    }

    private MatchingCandidate candidate(final Long userId) {
        return new MatchingCandidate(userId, FIXED_TIME);
    }
}
