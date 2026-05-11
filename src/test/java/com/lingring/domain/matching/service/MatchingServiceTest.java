package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingPollStatus;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.exception.MatchConfirmationNotFoundException;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class MatchingServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 12, 12, 0, 0);

    @MockitoBean
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private MatchingQueueRepository matchingQueueRepository;

    @Autowired
    private MatchConfirmationRepository matchConfirmationRepository;

    @Autowired
    private PairCooldownRepository pairCooldownRepository;

    @Autowired
    private CallRepository callRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private MatchingWorker matchingWorker;

    @BeforeEach
    void stubDefaultTime() {
        given(dateTimeProvider.now()).willReturn(FIXED_NOW);
    }

    private UUID seedConfirmation(final Long userA, final Long userB, final LocalDateTime deadline) {
        matchingQueueRepository.enqueue(userA, FIXED_NOW);
        matchingQueueRepository.enqueue(userB, FIXED_NOW);
        final UUID roomId = UUID.randomUUID();
        matchConfirmationRepository.commit(userA, userB, roomId, deadline);
        return roomId;
    }

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
            final UUID roomId = UUID.randomUUID();
            matchingQueueRepository.saveResult(1L, 2L, roomId);
            matchingQueueRepository.saveResult(2L, 1L, roomId);
            assertThat(matchingQueueRepository.findResult(1L)).isPresent();

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
        @DisplayName("매칭 결과가 있으면 MATCHED와 partnerId, roomId를 반환한다")
        void getStatus_whenMatched_returnsMatched() {
            // given: 워커가 페어링한 상태를 직접 시드
            final UUID roomId = UUID.randomUUID();
            matchingQueueRepository.saveResult(1L, 2L, roomId);
            matchingQueueRepository.saveResult(2L, 1L, roomId);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchingPollStatus.MATCHED);
            assertThat(response.partnerId()).isEqualTo(2L);
            assertThat(response.roomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("큐에만 있으면 WAITING을 반환한다")
        void getStatus_whenInQueue_returnsWaiting() {
            // given
            matchingService.enterQueue(1L);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchingPollStatus.WAITING);
            assertThat(response.partnerId()).isNull();
            assertThat(response.roomId()).isNull();
        }

        @Test
        @DisplayName("큐에도 없고 매칭 결과도 없으면 NONE을 반환한다")
        void getStatus_whenNone_returnsNone() {
            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchingPollStatus.NONE);
            assertThat(response.partnerId()).isNull();
            assertThat(response.roomId()).isNull();
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

    @Nested
    @DisplayName("getStatus: AWAITING_CONFIRM 분기")
    class GetStatusAwaitingConfirm {

        @Test
        @DisplayName("confirm 레코드가 있고 만료 전이면 AWAITING_CONFIRM과 partnerId·deadline을 반환한다")
        void getStatus_whenAwaitingConfirm_returnsAwaiting() {
            // given
            final LocalDateTime future = FIXED_NOW.plusSeconds(10);
            seedConfirmation(1L, 2L, future);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchingPollStatus.AWAITING_CONFIRM);
            assertThat(response.partnerId()).isEqualTo(2L);
            assertThat(response.confirmDeadline()).isEqualTo(future);
            assertThat(response.roomId()).isNull();
        }

        @Test
        @DisplayName("confirm 레코드가 만료된 상태로 polling 들어오면 lazy expire되어 WAITING + cooldown 적용")
        void getStatus_whenConfirmExpired_lazyExpires() {
            // given
            final LocalDateTime past = FIXED_NOW.minusSeconds(1);
            seedConfirmation(1L, 2L, past);

            // when
            final MatchingStatusResponse response = matchingService.getStatus(1L);

            // then
            assertThat(response.status()).isEqualTo(MatchingPollStatus.WAITING);
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
            assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).isTrue();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
        }
    }

    @Nested
    @DisplayName("acceptMatch: 매칭 수락")
    class AcceptMatch {

        @Test
        @DisplayName("한쪽만 accept하면 confirm은 유지되고 Call은 아직 저장되지 않는다")
        void acceptMatch_oneSide_keepsConfirm() {
            // given
            seedConfirmation(1L, 2L, FIXED_NOW.plusSeconds(10));

            // when
            matchingService.acceptMatch(1L);

            // then
            assertThat(matchConfirmationRepository.findByUser(1L)).isPresent();
            assertThat(callRepository.count()).isZero();
        }

        @Test
        @DisplayName("양쪽 모두 accept하면 result key 생성 + Call 저장 + confirm 삭제")
        void acceptMatch_bothSides_promotesAndPersistsCall() {
            // given
            final UUID roomId = seedConfirmation(1L, 2L, FIXED_NOW.plusSeconds(10));

            // when
            matchingService.acceptMatch(1L);
            matchingService.acceptMatch(2L);

            // then
            assertThat(matchingQueueRepository.findResult(1L)).isPresent();
            assertThat(matchingQueueRepository.findResult(1L).get().roomId()).isEqualTo(roomId);
            assertThat(callRepository.count()).isEqualTo(1L);
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
        }

        @Test
        @DisplayName("이미 만료된 confirm을 accept하면 expire 처리되고 cooldown 적용")
        void acceptMatch_whenExpired_expiresAndCools() {
            // given
            seedConfirmation(1L, 2L, FIXED_NOW.minusSeconds(1));

            // when
            matchingService.acceptMatch(1L);

            // then
            assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).isTrue();
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
        }

        @Test
        @DisplayName("confirm record가 없는 사용자가 accept 호출하면 MatchConfirmationNotFoundException")
        void acceptMatch_noConfirmation_throws() {
            // when & then
            assertThatThrownBy(() -> matchingService.acceptMatch(99L))
                    .isInstanceOf(MatchConfirmationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("declineMatch: 매칭 거절")
    class DeclineMatch {

        @Test
        @DisplayName("decline하면 confirm이 삭제되고 cooldown 적용, 양쪽 모두 큐에 재진입")
        void declineMatch_clearsAndRequeues() {
            // given
            seedConfirmation(1L, 2L, FIXED_NOW.plusSeconds(10));

            // when
            matchingService.declineMatch(1L);

            // then
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
            assertThat(matchConfirmationRepository.findByUser(2L)).isEmpty();
            assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).isTrue();
            assertThat(matchingQueueRepository.contains(1L)).isTrue();
            assertThat(matchingQueueRepository.contains(2L)).isTrue();
        }

        @Test
        @DisplayName("confirm record가 없는 사용자의 decline은 MatchConfirmationNotFoundException")
        void declineMatch_noConfirmation_throws() {
            // when & then
            assertThatThrownBy(() -> matchingService.declineMatch(99L))
                    .isInstanceOf(MatchConfirmationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("expireOverdueConfirmations: Worker용 만료 정리")
    class ExpireOverdue {

        @Test
        @DisplayName("만료된 confirm은 모두 정리되고 cooldown 적용, 미만료는 유지")
        void expireOverdue_processesAllExpired() {
            // given
            seedConfirmation(1L, 2L, FIXED_NOW.minusSeconds(1));
            seedConfirmation(3L, 4L, FIXED_NOW.plusSeconds(10));

            // when
            matchingService.expireOverdueConfirmations();

            // then
            assertThat(matchConfirmationRepository.findByUser(1L)).isEmpty();
            assertThat(matchConfirmationRepository.findByUser(3L)).isPresent();
            assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(1L, 2L))).isTrue();
            assertThat(pairCooldownRepository.contains(MatchConfirmation.pairKeyOf(3L, 4L))).isFalse();
        }
    }
}
