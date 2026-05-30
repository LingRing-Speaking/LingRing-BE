package com.lingring.domain.call.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.exception.CallParticipantMismatchException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 5, 2, 10, 0);
    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("start: 통화 기록 생성")
    class Start {

        @Test
        @DisplayName("두 userId 중 작은 쪽이 userA, 큰 쪽이 userB로 정렬된다")
        void start_sortsUserIds() {
            // when
            final Call call = Call.start(5L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(call.getUserAId()).isEqualTo(2L);
            assertThat(call.getUserBId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("입력 순서와 무관하게 동일하게 정렬된다")
        void start_resultsSameRegardlessOfArgumentOrder() {
            // when
            final Call a = Call.start(2L, 5L, ROOM_ID, STARTED_AT);
            final Call b = Call.start(5L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(a.getUserAId()).isEqualTo(b.getUserAId());
            assertThat(a.getUserBId()).isEqualTo(b.getUserBId());
        }

        @Test
        @DisplayName("초기 상태는 isActive=true, endedAt은 null이다")
        void start_initialStateIsActive() {
            // when
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(call.isActive()).isTrue();
            assertThat(call.getStartedAt()).isEqualTo(STARTED_AT);
            assertThat(call.getEndedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("end: 통화 종료")
    class End {

        @Test
        @DisplayName("진행 중인 통화에 end를 호출하면 isActive=false가 되고 endedAt이 설정된다")
        void end_whenActive_marksEnded() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);
            final LocalDateTime endedAt = STARTED_AT.plusMinutes(5);

            // when
            call.end(endedAt);

            // then
            assertThat(call.isActive()).isFalse();
            assertThat(call.getEndedAt()).isEqualTo(endedAt);
        }

        @Test
        @DisplayName("end 호출 시 durationSec이 endedAt - startedAt 초로 계산되어 저장된다")
        void end_setsDurationSec() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when
            call.end(STARTED_AT.plusSeconds(312));

            // then
            assertThat(call.getDurationSec()).isEqualTo(312L);
        }

        @Test
        @DisplayName("이미 종료된 통화에 end를 호출해도 endedAt이 변하지 않는다 (멱등)")
        void end_whenAlreadyEnded_isIdempotent() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);
            final LocalDateTime firstEnd = STARTED_AT.plusMinutes(5);
            final LocalDateTime secondEnd = STARTED_AT.plusMinutes(10);
            call.end(firstEnd);

            // when
            call.end(secondEnd);

            // then
            assertThat(call.isActive()).isFalse();
            assertThat(call.getEndedAt()).isEqualTo(firstEnd);
        }

        @Test
        @DisplayName("이미 종료된 통화에 end를 다시 호출해도 durationSec이 변하지 않는다 (멱등)")
        void end_whenAlreadyEnded_doesNotRecomputeDurationSec() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);
            call.end(STARTED_AT.plusMinutes(5));
            final Long firstDurationSec = call.getDurationSec();

            // when
            call.end(STARTED_AT.plusMinutes(10));

            // then
            assertThat(call.getDurationSec()).isEqualTo(firstDurationSec);
        }
    }

    @Nested
    @DisplayName("involves / counterpartOf / caller / callee")
    class Participation {

        @Test
        @DisplayName("involves는 참여자에 대해 true, 비참여자에 대해 false를 반환한다")
        void involves_returnsTrueForParticipant() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(call.involves(1L)).isTrue();
            assertThat(call.involves(2L)).isTrue();
            assertThat(call.involves(99L)).isFalse();
        }

        @Test
        @DisplayName("counterpartOf는 상대 userId를 반환한다")
        void counterpartOf_returnsOther() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(call.counterpartOf(1L)).isEqualTo(2L);
            assertThat(call.counterpartOf(2L)).isEqualTo(1L);
        }

        @Test
        @DisplayName("counterpartOf는 비참여자에 대해 CallParticipantMismatchException을 던진다")
        void counterpartOf_whenNotParticipant_throws() {
            // given
            final Call call = Call.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThatThrownBy(() -> call.counterpartOf(99L))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }

        @Test
        @DisplayName("callerUserId는 userA(작은 쪽), calleeUserId는 userB(큰 쪽)을 반환한다")
        void callerAndCallee_followIdOrdering() {
            // given
            final Call call = Call.start(5L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(call.callerUserId()).isEqualTo(2L);
            assertThat(call.calleeUserId()).isEqualTo(5L);
        }
    }
}
