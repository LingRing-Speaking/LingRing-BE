package com.lingring.domain.matching.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlockExclusionPolicyTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Autowired
    private BlockExclusionPolicy blockExclusionPolicy;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Nested
    @DisplayName("filterFor: self 기준 매칭 가능 여부 Predicate 생성")
    class FilterFor {

        @Test
        @DisplayName("차단 관계가 없으면 true를 반환한다")
        void returnsTrue_whenNoBlockRelation() {
            // given
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final Predicate<MatchingCandidate> filter = blockExclusionPolicy.filterFor(self);

            // then
            assertThat(filter.test(other)).isTrue();
        }

        @Test
        @DisplayName("self가 other를 차단했으면 false를 반환한다")
        void returnsFalse_whenSelfBlockedOther() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final Predicate<MatchingCandidate> filter = blockExclusionPolicy.filterFor(self);

            // then
            assertThat(filter.test(other)).isFalse();
        }

        @Test
        @DisplayName("other가 self를 차단했으면 false를 반환한다 (양방향 검증)")
        void returnsFalse_whenOtherBlockedSelf() {
            // given
            userBlockRepository.save(UserBlock.create(2L, 1L));
            final MatchingCandidate self = candidate(1L);
            final MatchingCandidate other = candidate(2L);

            // when
            final Predicate<MatchingCandidate> filter = blockExclusionPolicy.filterFor(self);

            // then
            assertThat(filter.test(other)).isFalse();
        }

        @Test
        @DisplayName("서로 차단하지 않은 다른 후보는 true, 차단된 후보만 false를 반환한다")
        void filtersOnlyBlockedCandidates() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));
            final MatchingCandidate self = candidate(1L);

            // when
            final Predicate<MatchingCandidate> filter = blockExclusionPolicy.filterFor(self);

            // then
            assertThat(filter.test(candidate(2L))).isFalse();
            assertThat(filter.test(candidate(3L))).isTrue();
            assertThat(filter.test(candidate(4L))).isTrue();
        }
    }

    private MatchingCandidate candidate(final Long userId) {
        return new MatchingCandidate(userId, FIXED_TIME);
    }
}
