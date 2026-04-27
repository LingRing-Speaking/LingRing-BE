package com.lingring.domain.matching.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("filterCandidates: 양방향 차단 후보 제외")
    class FilterCandidates {

        @Test
        @DisplayName("차단 관계가 없으면 모든 후보가 그대로 반환된다")
        void returnsAll_whenNoBlockRelation() {
            // given
            final MatchingCandidate self = candidate(1L);
            final List<MatchingCandidate> candidates = List.of(candidate(2L), candidate(3L));

            // when
            final List<MatchingCandidate> result = blockExclusionPolicy.filterCandidates(self, candidates);

            // then
            assertThat(result).extracting(MatchingCandidate::userId).containsExactly(2L, 3L);
        }

        @Test
        @DisplayName("self가 차단한 후보가 제외된다")
        void excludes_whenSelfBlockedCandidate() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));
            final MatchingCandidate self = candidate(1L);
            final List<MatchingCandidate> candidates = List.of(candidate(2L), candidate(3L));

            // when
            final List<MatchingCandidate> result = blockExclusionPolicy.filterCandidates(self, candidates);

            // then
            assertThat(result).extracting(MatchingCandidate::userId).containsExactly(3L);
        }

        @Test
        @DisplayName("self를 차단한 후보가 제외된다 (양방향)")
        void excludes_whenCandidateBlockedSelf() {
            // given
            userBlockRepository.save(UserBlock.create(2L, 1L));
            final MatchingCandidate self = candidate(1L);
            final List<MatchingCandidate> candidates = List.of(candidate(2L), candidate(3L));

            // when
            final List<MatchingCandidate> result = blockExclusionPolicy.filterCandidates(self, candidates);

            // then
            assertThat(result).extracting(MatchingCandidate::userId).containsExactly(3L);
        }

        @Test
        @DisplayName("후보 목록이 비어있으면 빈 리스트를 반환한다")
        void returnsEmpty_whenCandidatesEmpty() {
            // given
            final MatchingCandidate self = candidate(1L);

            // when
            final List<MatchingCandidate> result = blockExclusionPolicy.filterCandidates(self, List.of());

            // then
            assertThat(result).isEmpty();
        }
    }

    private MatchingCandidate candidate(final Long userId) {
        return new MatchingCandidate(userId, FIXED_TIME);
    }
}
