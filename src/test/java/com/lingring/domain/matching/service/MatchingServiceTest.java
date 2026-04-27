package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.domain.MatchStatus;
import com.lingring.domain.matching.domain.MatchingPollStatus;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.exception.MatchNotFoundException;
import com.lingring.domain.matching.scheduler.MatchingWorker;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
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

    @Autowired
    private MatchRepository matchRepository;

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
    @DisplayName("endMatch: 매칭 종료")
    class EndMatch {

        @Test
        @DisplayName("STARTED 상태인 Match를 ENDED로 변경하고 endedAt을 기록한다")
        void endMatch_changesStatusToEnded() {
            // given
            final UUID roomId = UUID.randomUUID();
            final LocalDateTime startedAt = LocalDateTime.of(2026, 4, 28, 10, 0);
            matchRepository.save(Match.start(1L, 2L, roomId, startedAt));

            // when
            matchingService.endMatch(roomId);

            // then
            final Optional<Match> ended = matchRepository.findByRoomId(roomId);
            assertThat(ended).isPresent();
            assertThat(ended.get().getStatus()).isEqualTo(MatchStatus.ENDED);
            assertThat(ended.get().getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 roomId면 MatchNotFoundException을 던진다")
        void endMatch_whenRoomIdNotFound_throws() {
            // given
            final UUID unknownRoomId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() -> matchingService.endMatch(unknownRoomId))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        @DisplayName("이미 종료된 Match에 다시 호출해도 endedAt이 변하지 않는다 (멱등)")
        void endMatch_whenAlreadyEnded_isIdempotent() {
            // given
            final UUID roomId = UUID.randomUUID();
            final LocalDateTime startedAt = LocalDateTime.of(2026, 4, 28, 10, 0);
            matchRepository.save(Match.start(1L, 2L, roomId, startedAt));
            matchingService.endMatch(roomId);
            final LocalDateTime firstEndedAt = matchRepository.findByRoomId(roomId).get().getEndedAt();

            // when
            matchingService.endMatch(roomId);

            // then
            final Match ended = matchRepository.findByRoomId(roomId).orElseThrow();
            assertThat(ended.getStatus()).isEqualTo(MatchStatus.ENDED);
            assertThat(ended.getEndedAt()).isEqualTo(firstEndedAt);
        }
    }
}