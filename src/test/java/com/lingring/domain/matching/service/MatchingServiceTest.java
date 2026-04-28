package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.global.config.ServiceIntegrationHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MatchingServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchingWorker matchingWorker;

    @Nested
    @DisplayName("enterQueue: 매칭 대기열 입장")
    class EnterQueue {

        @Test
        @DisplayName("입장 시 큐에 적재된다")
        void enter_addsUserToQueue() {
            // when
            matchingService.enterQueue(1L);

            // then
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
        }

        @Test
        @DisplayName("입장은 즉시 매칭하지 않으므로 두 사용자가 동시에 입장해도 양쪽 모두 큐에 남는다")
        void enter_doesNotMatchEvenIfPartnerWaiting() {
            // when
            matchingService.enterQueue(1L);
            matchingService.enterQueue(2L);

            // then
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.findResult(2L)).isEmpty();
        }

        @Test
        @DisplayName("이전 매칭 결과가 남아있어도 다시 입장하면 결과가 클리어되고 큐에 적재된다")
        void enter_clearsPreviousResult() {
            // given: 이전에 1L-2L이 매칭됐다고 가정한 상태
            matchingQueueRepository.commitMatch(1L, 2L);
            assertThat(matchingQueueRepository.findResult(1L)).contains(2L);

            // when
            matchingService.enterQueue(1L);

            // then
            assertThat(matchingQueueRepository.findResult(1L)).isEmpty();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
        }

        @Test
        @DisplayName("이미 큐에 있는 사용자가 다시 입장해도 예외 없이 다시 적재된다 (멱등)")
        void enter_whenAlreadyInQueue_remainsInQueue() {
            // given
            matchingService.enterQueue(1L);

            // when
            matchingService.enterQueue(1L);

            // then
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("getStatus: 매칭 상태 조회")
    class GetStatus {

        @Test
        @DisplayName("매칭 결과가 있으면 MATCHED를 반환한다")
        void getStatus_whenMatched_returnsMatched() {
            // given: 워커가 페어링한 상태를 직접 시드
            matchingQueueRepository.commitMatch(1L, 2L);

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
