package com.lingring.domain.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchingQueueTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Nested
    @DisplayName("findPartnerFor")
    class FindPartnerFor {

        @Test
        @DisplayName("self를 제외한 후보 중 정책을 모두 통과한 첫 번째 후보를 반환한다")
        void returnsFirstCompatibleCandidate() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of(
                    candidate(1L), candidate(2L), candidate(3L)
            ));
            final MatchingPolicies acceptAll = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates
            ));

            // when
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), acceptAll, Set.of());

            // then
            assertThat(partner).isPresent();
            assertThat(partner.get().userId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("정책이 모두 거부하면 빈 Optional을 반환한다")
        void returnsEmpty_whenAllRejectedByPolicies() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of(candidate(2L), candidate(3L)));
            final MatchingPolicies rejectAll = new MatchingPolicies(List.of(
                    (self, candidates) -> List.of()
            ));

            // when
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), rejectAll, Set.of());

            // then
            assertThat(partner).isEmpty();
        }

        @Test
        @DisplayName("큐에 self만 있으면 빈 Optional을 반환한다")
        void returnsEmpty_whenOnlySelfInQueue() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of(candidate(1L)));
            final MatchingPolicies acceptAll = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates
            ));

            // when
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), acceptAll, Set.of());

            // then
            assertThat(partner).isEmpty();
        }

        @Test
        @DisplayName("큐가 비어있으면 빈 Optional을 반환한다")
        void returnsEmpty_whenQueueEmpty() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of());
            final MatchingPolicies acceptAll = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates
            ));

            // when
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), acceptAll, Set.of());

            // then
            assertThat(partner).isEmpty();
        }

        @Test
        @DisplayName("정책 통과한 첫 후보가 self보다 뒤에 있어도 self는 제외된다")
        void excludesSelfEvenIfNotFirst() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of(
                    candidate(2L), candidate(1L), candidate(3L)
            ));
            final MatchingPolicies acceptAll = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates
            ));

            // when
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), acceptAll, Set.of());

            // then: self(1L) 제외 후 [2, 3], 첫 번째 → 2
            assertThat(partner).isPresent();
            assertThat(partner.get().userId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("excludedIds에 포함된 후보는 정책 적용 전에 제외된다")
        void excludesCandidatesInExcludedIds() {
            // given
            final MatchingQueue queue = new MatchingQueue(List.of(
                    candidate(2L), candidate(3L), candidate(4L)
            ));
            final MatchingPolicies acceptAll = new MatchingPolicies(List.of(
                    (self, candidates) -> candidates
            ));

            // when: 2L은 같은 틱에서 이미 다른 페어에 소비된 상태
            final Optional<MatchingCandidate> partner =
                    queue.findPartnerFor(candidate(1L), acceptAll, Set.of(2L));

            // then: 2L 건너뛰고 다음 후보 3L 반환
            assertThat(partner).isPresent();
            assertThat(partner.get().userId()).isEqualTo(3L);
        }
    }

    private MatchingCandidate candidate(final Long userId) {
        return new MatchingCandidate(userId, FIXED_TIME);
    }
}
