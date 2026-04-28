package com.lingring.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RedisMatchingQueueRepositoryTest extends ServiceIntegrationHelper {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchingWorker matchingWorker;

    @Nested
    @DisplayName("enqueue / contains / remove")
    class QueueOperations {

        @Test
        @DisplayName("enqueue 후 contains는 true를 반환한다")
        void enqueue_thenContainsReturnsTrue() {
            // given
            matchingQueueRepository.enqueue(1L, BASE_TIME);

            // when
            final boolean contains = matchingQueueRepository.contains(1L);

            // then
            assertThat(contains).isTrue();
        }

        @Test
        @DisplayName("enqueue 안 한 사용자에 대해 contains는 false를 반환한다")
        void contains_whenNotEnqueued_returnsFalse() {
            // when
            final boolean contains = matchingQueueRepository.contains(99L);

            // then
            assertThat(contains).isFalse();
        }

        @Test
        @DisplayName("remove 후 contains는 false를 반환한다")
        void remove_thenContainsReturnsFalse() {
            // given
            matchingQueueRepository.enqueue(1L, BASE_TIME);

            // when
            matchingQueueRepository.remove(1L);

            // then
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
        }

        @Test
        @DisplayName("같은 userId로 다시 enqueue하면 score(입장 시각)가 갱신된다")
        void enqueue_sameUser_updatesScore() {
            // given
            matchingQueueRepository.enqueue(1L, BASE_TIME);
            final LocalDateTime later = BASE_TIME.plusSeconds(5);

            // when
            matchingQueueRepository.enqueue(1L, later);

            // then
            final List<MatchingCandidate> all = matchingQueueRepository.findAllOrderByEnqueuedAt();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).enqueuedAt()).isEqualTo(later);
        }
    }

    @Nested
    @DisplayName("findAllOrderByEnqueuedAt")
    class FindAll {

        @Test
        @DisplayName("입장 시각 오름차순으로 모든 후보를 반환한다")
        void returnsAllCandidatesInEnqueueOrder() {
            // given
            matchingQueueRepository.enqueue(2L, BASE_TIME.plusSeconds(2));
            matchingQueueRepository.enqueue(1L, BASE_TIME);
            matchingQueueRepository.enqueue(3L, BASE_TIME.plusSeconds(1));

            // when
            final List<MatchingCandidate> all = matchingQueueRepository.findAllOrderByEnqueuedAt();

            // then
            assertThat(all).extracting(MatchingCandidate::userId)
                    .containsExactly(1L, 3L, 2L);
        }

        @Test
        @DisplayName("큐가 비어있으면 빈 리스트를 반환한다")
        void returnsEmptyWhenQueueEmpty() {
            // when
            final List<MatchingCandidate> all = matchingQueueRepository.findAllOrderByEnqueuedAt();

            // then
            assertThat(all).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveResult / findResult / clearResult")
    class ResultOperations {

        @Test
        @DisplayName("saveResult 후 findResult는 partnerId를 반환한다")
        void saveAndFindResult() {
            // given
            matchingQueueRepository.saveResult(1L, 2L);

            // when
            final Optional<Long> result = matchingQueueRepository.findResult(1L);

            // then
            assertThat(result).contains(2L);
        }

        @Test
        @DisplayName("저장 안 된 사용자의 findResult는 빈 Optional을 반환한다")
        void findResult_whenNotSaved_returnsEmpty() {
            // when
            final Optional<Long> result = matchingQueueRepository.findResult(99L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("clearResult 후 findResult는 빈 Optional을 반환한다")
        void clearResult_thenFindReturnsEmpty() {
            // given
            matchingQueueRepository.saveResult(1L, 2L);

            // when
            matchingQueueRepository.clearResult(1L);

            // then
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("commitMatch")
    class CommitMatch {

        @Test
        @DisplayName("두 사용자가 모두 큐에 있으면 commit 성공: true 반환 + 양쪽 큐 제거 + 양쪽 result 저장")
        void commit_whenBothInQueue_savesResultsAndReturnsTrue() {
            // given
            matchingQueueRepository.enqueue(1L, BASE_TIME);
            matchingQueueRepository.enqueue(2L, BASE_TIME.plusSeconds(1));

            // when
            final boolean committed = matchingQueueRepository.commitMatch(1L, 2L);

            // then
            assertThat(committed).isTrue();
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            assertThat(matchingQueueRepository.findResult(1L)).contains(2L);
            assertThat(matchingQueueRepository.findResult(2L)).contains(1L);
        }

        @Test
        @DisplayName("partner가 큐에 없으면 commit 실패: false 반환 + self 큐에 그대로 + result 저장 안 함")
        void commit_whenPartnerLeftBeforeCommit_returnsFalseAndNoResultStored() {
            // given: 1L은 큐에 있지만 2L은 이미 leaveQueue로 빠진 상태
            matchingQueueRepository.enqueue(1L, BASE_TIME);

            // when
            final boolean committed = matchingQueueRepository.commitMatch(1L, 2L);

            // then
            assertThat(committed).isFalse();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.findResult(2L)).isEmpty();
        }

        @Test
        @DisplayName("self가 큐에 없으면 commit 실패: false 반환 + partner 큐에 그대로 + result 저장 안 함")
        void commit_whenUserLeftBeforeCommit_returnsFalseAndNoResultStored() {
            // given
            matchingQueueRepository.enqueue(2L, BASE_TIME);

            // when
            final boolean committed = matchingQueueRepository.commitMatch(1L, 2L);

            // then
            assertThat(committed).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.findResult(2L)).isEmpty();
        }

        @Test
        @DisplayName("두 사용자 모두 큐에 없으면 commit 실패: false 반환 + result 저장 안 함")
        void commit_whenNeitherInQueue_returnsFalseAndNoResultStored() {
            // when
            final boolean committed = matchingQueueRepository.commitMatch(1L, 2L);

            // then
            assertThat(committed).isFalse();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.findResult(2L)).isEmpty();
        }
    }
}
