package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MatchingExecutorTest extends ServiceIntegrationHelper {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 4, 27, 10, 0);

    @Autowired
    private MatchingExecutor matchingExecutor;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @Autowired
    private MatchConfirmationRepository matchConfirmationRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

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
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
        }

        @Test
        @DisplayName("두 사용자가 큐에 있으면 입장 시각 순으로 confirm record를 생성하고 양쪽 user index를 등록한다")
        void executeRound_pairsTwoUsersByEnqueuedAtOrder() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));

            // when
            matchingExecutor.executeRound();

            // then
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            final Optional<MatchConfirmation> c1 = matchConfirmationRepository.findByUser(1L);
            final Optional<MatchConfirmation> c2 = matchConfirmationRepository.findByUser(2L);
            assertThat(c1).isPresent();
            assertThat(c2).isPresent();
            assertThat(c1.get().pairKey()).isEqualTo("1:2");
            assertThat(c1.get().roomId()).isEqualTo(c2.get().roomId());
        }

        @Test
        @DisplayName("페어링 성사 시점에는 아직 Call 엔티티가 저장되지 않는다 (accept 완료 시점에 저장)")
        void executeRound_doesNotPersistCallYet() {
            // given
            matchingQueueRepository.enqueue(1L, BASE);
            matchingQueueRepository.enqueue(2L, BASE.plusSeconds(1));

            // when
            matchingExecutor.executeRound();

            // then
            assertThat(callRepository.count()).isZero();
            assertThat(matchConfirmationRepository.findByUser(1L)).isPresent();
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
            assertThat(matchConfirmationRepository.findByUser(3L))
                    .map(c -> c.partnerOf(3L))
                    .contains(2L);
            assertThat(matchConfirmationRepository.findByUser(2L))
                    .map(c -> c.partnerOf(2L))
                    .contains(3L);
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
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
            assertThat(matchConfirmationRepository.findByUser(1L))
                    .map(c -> c.partnerOf(1L))
                    .contains(2L);
            assertThat(matchConfirmationRepository.findByUser(2L))
                    .map(c -> c.partnerOf(2L))
                    .contains(1L);
            assertThat(matchConfirmationRepository.findByUser(3L))
                    .map(c -> c.partnerOf(3L))
                    .contains(4L);
            assertThat(matchConfirmationRepository.findByUser(4L))
                    .map(c -> c.partnerOf(4L))
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
            assertThat(matchConfirmationRepository.findByUser(1L))
                    .map(c -> c.partnerOf(1L))
                    .contains(2L);
            assertThat(matchConfirmationRepository.findByUser(3L)).isEmpty();
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
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
            assertThat(matchConfirmationRepository.findByUser(2L)).isEmpty();
        }
    }
}
