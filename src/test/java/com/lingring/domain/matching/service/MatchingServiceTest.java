package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MatchingServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Nested
    @DisplayName("enterQueue: 매칭 대기열 입장")
    class EnterQueue {

        @Test
        @DisplayName("큐가 비어있을 때 입장하면 WAITING을 반환하고 큐에 적재된다")
        void enter_whenQueueEmpty_returnsWaiting() {
            // when
            final MatchingStatusResponse response = matchingService.enterQueue(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
            assertThat(response.partnerId()).isNull();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 사용자가 대기 중일 때 입장하면 즉시 매칭되고 양쪽 큐에서 모두 제거된다")
        void enter_whenPartnerWaiting_returnsMatched() {
            // given
            matchingService.enterQueue(1L);

            // when
            final MatchingStatusResponse response = matchingService.enterQueue(2L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.MATCHED);
            assertThat(response.partnerId()).isEqualTo(1L);
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
        }

        @Test
        @DisplayName("매칭 성사 시 양쪽 사용자에게 매칭 결과가 저장된다")
        void enter_whenMatched_savesResultForBothUsers() {
            // given
            matchingService.enterQueue(1L);

            // when
            matchingService.enterQueue(2L);

            // then
            assertThat(matchingQueueRepository.findResult(1L)).contains(2L);
            assertThat(matchingQueueRepository.findResult(2L)).contains(1L);
        }

        @Test
        @DisplayName("내가 차단한 사용자만 큐에 있으면 매칭되지 않고 WAITING을 반환한다")
        void enter_whenOnlyBlockedCandidatesAvailable_returnsWaiting() {
            // given
            userBlockRepository.save(UserBlock.create(2L, 1L));
            matchingService.enterQueue(1L);

            // when
            final MatchingStatusResponse response = matchingService.enterQueue(2L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
        }

        @Test
        @DisplayName("나를 차단한 사용자만 큐에 있어도 매칭되지 않는다 (양방향 차단)")
        void enter_whenBlockedBySelfsCandidates_returnsWaiting() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));
            matchingService.enterQueue(1L);

            // when
            final MatchingStatusResponse response = matchingService.enterQueue(2L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
        }

        @Test
        @DisplayName("차단된 후보와 차단되지 않은 후보가 큐에 모두 있으면 차단되지 않은 후보와 매칭된다")
        void enter_whenBlockedAndNonBlockedExist_picksNonBlocked() {
            // given: 3L이 1L을 차단. 큐에 3L(먼저)과 2L이 적재되어 있음
            userBlockRepository.save(UserBlock.create(3L, 1L));
            final LocalDateTime base = LocalDateTime.of(2026, 4, 27, 10, 0);
            matchingQueueRepository.enqueue(3L, base);
            matchingQueueRepository.enqueue(2L, base.plusSeconds(1));

            // when: 1L 입장
            final MatchingStatusResponse response = matchingService.enterQueue(1L);

            // then: 3L은 양방향 차단으로 제외되어 1L은 2L과 매칭됨, 3L은 큐에 그대로 남음
            assertThat(response.status()).isEqualTo(MatchStatus.MATCHED);
            assertThat(response.partnerId()).isEqualTo(2L);
            assertThat(matchingQueueRepository.contains(2L)).isFalse();
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
            assertThat(matchingQueueRepository.contains(3L)).isTrue();
        }

        @Test
        @DisplayName("이미 큐에 있는 사용자가 다시 입장하면 입장 시각이 갱신되고 새 후보 탐색을 다시 시도한다")
        void enter_whenAlreadyInQueue_refreshesAndRetries() {
            // given
            matchingService.enterQueue(1L);

            // when: 같은 사용자가 다시 호출
            final MatchingStatusResponse response = matchingService.enterQueue(1L);

            // then: 큐에 다른 후보 없음 → WAITING (자기 자신과는 매칭 안 됨)
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
        }

        @Test
        @DisplayName("매칭 성사 후 다시 입장 시도하면 기존 결과가 클리어되고 새로 매칭 시도한다")
        void enter_afterMatched_clearsPreviousResult() {
            // given
            matchingService.enterQueue(1L);
            matchingService.enterQueue(2L);
            assertThat(matchingQueueRepository.findResult(1L)).contains(2L);

            // when
            final MatchingStatusResponse response = matchingService.enterQueue(1L);

            // then: 이전 결과 클리어 + 큐 비어있어 WAITING
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStatus: 매칭 상태 조회")
    class GetStatus {

        @Test
        @DisplayName("매칭 결과가 있으면 MATCHED를 반환한다")
        void getStatus_whenMatched_returnsMatched() {
            // given
            matchingService.enterQueue(1L);
            matchingService.enterQueue(2L);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.MATCHED);
            assertThat(response.partnerId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("큐에만 있으면 WAITING을 반환한다")
        void getStatus_whenInQueue_returnsWaiting() {
            // given
            matchingService.enterQueue(1L);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.WAITING);
            assertThat(response.partnerId()).isNull();
        }

        @Test
        @DisplayName("큐에도 없고 매칭 결과도 없으면 NONE을 반환한다")
        void getStatus_whenNone_returnsNone() {
            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchStatus.NONE);
            assertThat(response.partnerId()).isNull();
        }
    }

    @Nested
    @DisplayName("leaveQueue: 매칭 대기열 취소")
    class LeaveQueue {

        @Test
        @DisplayName("큐에 있는 사용자를 제거한다")
        void leave_removesFromQueue() {
            // given
            matchingService.enterQueue(1L);

            // when
            matchingService.leaveQueue(1L);

            // then
            assertThat(matchingQueueRepository.contains(1L)).isFalse();
        }

        @Test
        @DisplayName("큐에 없는 사용자를 취소해도 예외가 발생하지 않는다 (멱등)")
        void leave_whenNotInQueue_doesNotThrow() {
            // when & then
            matchingService.leaveQueue(99L);
        }
    }
}
