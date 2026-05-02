package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallHistoryRepository;
import com.lingring.domain.call.domain.CallHistory;
import com.lingring.domain.call.exception.CallHistoryNotFoundException;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CallHistoryServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 5, 2, 10, 0);

    @Autowired
    private CallHistoryService callHistoryService;

    @Autowired
    private CallHistoryRepository callHistoryRepository;

    @Nested
    @DisplayName("findByRoomId: 통화 기록 조회 (선택적)")
    class FindByRoomId {

        @Test
        @DisplayName("roomId에 통화 기록이 있으면 Optional에 담아 반환한다")
        void findByRoomId_whenPresent_returnsCallHistory() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

            // when
            final Optional<CallHistory> found = callHistoryService.findByRoomId(roomId);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getRoomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("roomId에 통화 기록이 없으면 빈 Optional을 반환한다")
        void findByRoomId_whenAbsent_returnsEmpty() {
            // when
            final Optional<CallHistory> found = callHistoryService.findByRoomId(UUID.randomUUID());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("getByRoomId: 통화 기록 조회 (필수)")
    class GetByRoomId {

        @Test
        @DisplayName("roomId에 통화 기록이 있으면 CallHistory를 반환한다")
        void getByRoomId_whenPresent_returnsCallHistory() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

            // when
            final CallHistory found = callHistoryService.getByRoomId(roomId);

            // then
            assertThat(found.getRoomId()).isEqualTo(roomId);
        }

        @Test
        @DisplayName("roomId에 통화 기록이 없으면 CallHistoryNotFoundException을 던진다")
        void getByRoomId_whenAbsent_throws() {
            // when & then
            assertThatThrownBy(() -> callHistoryService.getByRoomId(UUID.randomUUID()))
                    .isInstanceOf(CallHistoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("endCall: 통화 종료")
    class EndCall {

        @Test
        @DisplayName("진행 중인 통화 기록에 호출하면 isActive=false가 되고 endedAt이 기록된다")
        void endCall_marksCallEnded() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));

            // when
            callHistoryService.endCall(roomId);

            // then
            final Optional<CallHistory> ended = callHistoryRepository.findByRoomId(roomId);
            assertThat(ended).isPresent();
            assertThat(ended.get().isActive()).isFalse();
            assertThat(ended.get().getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 roomId면 CallHistoryNotFoundException을 던진다")
        void endCall_whenRoomIdNotFound_throws() {
            // given
            final UUID unknownRoomId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() -> callHistoryService.endCall(unknownRoomId))
                    .isInstanceOf(CallHistoryNotFoundException.class);
        }

        @Test
        @DisplayName("이미 종료된 통화 기록에 다시 호출해도 endedAt이 변하지 않는다 (멱등)")
        void endCall_whenAlreadyEnded_isIdempotent() {
            // given
            final UUID roomId = UUID.randomUUID();
            callHistoryRepository.save(CallHistory.start(1L, 2L, roomId, STARTED_AT));
            callHistoryService.endCall(roomId);
            final LocalDateTime firstEndedAt = callHistoryRepository.findByRoomId(roomId)
                    .orElseThrow().getEndedAt();

            // when
            callHistoryService.endCall(roomId);

            // then
            final CallHistory ended = callHistoryRepository.findByRoomId(roomId).orElseThrow();
            assertThat(ended.isActive()).isFalse();
            assertThat(ended.getEndedAt()).isEqualTo(firstEndedAt);
        }
    }
}
