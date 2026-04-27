package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MatchingExecutorTest extends ServiceIntegrationHelper {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Autowired
    private MatchingExecutor matchingExecutor;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchingWorker matchingWorker;

    @Nested
    @DisplayName("executeRound: 한 라운드 페어링")
    class ExecuteRound {

        @Test
        @DisplayName("큐가 비어있으면 아무 일도 하지 않는다")
        void executeRound_whenQueueEmpty_doesNothing() {
            // when
            matchingExecutor.executeRound();

            // then
            assertThat(matchingQueueRepository.findAllOrderByEnqueuedAt()).isEmpty();
        }

        @Test
        @DisplayName("큐에 한 명만 있으면 페어링하지 않고 그대로 둔다")
        void executeRound_whenSingleUser_keepsInQueue() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);

            // when
            matchingExecutor.executeRound();

            // then
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
        }

        @Test
        @DisplayName("두 사용자가 큐에 있으면 입장 시각 순으로 페어링하고 양쪽 결과를 동일한 roomId로 저장한다")
        void executeRound_pairsTwoUsersByEnqueuedAtOrder() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));

            // when
            matchingExecutor.executeRound();

            // then
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            final Optional<MatchingResult> result1 = matchingQueueRepository.findResult(1L);
            final Optional<MatchingResult> result2 = matchingQueueRepository.findResult(2L);
            assertThat(result1).isPresent();
            assertThat(result1.get().partnerId()).isEqualTo(2L);
            assertThat(result2).isPresent();
            assertThat(result2.get().partnerId()).isEqualTo(1L);
            assertThat(result1.get().roomId()).isEqualTo(result2.get().roomId());
        }

        @Test
        @DisplayName("페어링 성사 시 Match 엔티티가 STARTED 상태로 영속화된다")
        void executeRound_persistsMatchEntity() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));

            // when
            matchingExecutor.executeRound();

            // then
            final Optional<MatchingResult> result = matchingQueueRepository.findResult(1L);
            assertThat(result).isPresent();
            final Optional<Match> match = matchRepository.findByRoomId(result.get().roomId());
            assertThat(match).isPresent();
            assertThat(match.get().getStatus()).isEqualTo(MatchStatus.STARTED);
            assertThat(match.get().getUserAId()).isEqualTo(1L);
            assertThat(match.get().getUserBId()).isEqualTo(2L);
            assertThat(match.get().getEndedAt()).isNull();
        }

        @Test
        @DisplayName("차단된 페어는 스킵하고 차단되지 않은 페어와 매칭된다")
        void executeRound_skipsBlockedPair_andPairsRemaining() {
            // given: 3L이 1L을 차단 → 1L과 3L은 매칭 불가
            userBlockRepository.save(UserBlock.create(3L, 1L));
            matchingQueueRepository.enqueue(3L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));
            matchingQueueRepository.enqueue(1L, BASE.plusSeconds(2));

            // when
            matchingExecutor.executeRound();

            // then: head=3L → 후보 [2L, 1L] 중 차단 관계 없는 2L과 매칭. 1L은 잔류
            assertThat(matchingQueueRepository.contains(3L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.findResult(3L))
                    .map(MatchingResult::partnerId)
                    .contains(2L);
            assertThat(matchingQueueRepository.findResult(2L))
                    .map(MatchingResult::partnerId)
                    .contains(3L);
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
        }

        @Test
        @DisplayName("한 라운드에 여러 페어를 동시에 매칭한다")
        void executeRound_pairsMultiplePairsInOneRound() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));
            matchingQueueRepository.enqueue(3L, BASE.plusSeconds(2));
            matchingQueueRepository.enqueue(4L, BASE.plusSeconds(3));

            // when
            matchingExecutor.executeRound();

            // then: 1-2, 3-4 두 쌍이 매칭되어 큐가 비어야 함
            assertThat(matchingQueueRepository.findAllOrderByEnqueuedAt()).isEmpty();
            assertThat(matchingQueueRepository.findResult(1L))
                    .map(MatchingResult::partnerId)
                    .contains(2L);
            assertThat(matchingQueueRepository.findResult(2L))
                    .map(MatchingResult::partnerId)
                    .contains(1L);
            assertThat(matchingQueueRepository.findResult(3L))
                    .map(MatchingResult::partnerId)
                    .contains(4L);
            assertThat(matchingQueueRepository.findResult(4L))
                    .map(MatchingResult::partnerId)
                    .contains(3L);
        }

        @Test
        @DisplayName("같은 사용자가 한 라운드에 여러 페어로 들어가지 않는다")
        void executeRound_doesNotConsumeSameUserTwice() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));
            matchingQueueRepository.enqueue(3L, BASE.plusSeconds(2));

            // when
            matchingExecutor.executeRound();

            // then: 1-2 페어링 후 3L은 자기와 매칭할 수 없으니 잔류
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            assertThat(matchingQueueRepository.contains(3L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L))
                    .map(MatchingResult::partnerId)
                    .contains(2L);
            assertThat(matchingQueueRepository.findResult(3L)).isEmpty();
        }

        @Test
        @DisplayName("양방향 차단 관계인 두 사용자만 큐에 있으면 페어링하지 않는다")
        void executeRound_whenAllBlocked_doesNotPair() {
            // given: 1L과 2L 양방향 차단
            userBlockRepository.save(UserBlock.create(1L, 2L));
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));

            // when
            matchingExecutor.executeRound();

            // then
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.findResult(2L)).isEmpty();
        }
    }
}