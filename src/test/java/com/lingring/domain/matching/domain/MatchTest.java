package com.lingring.domain.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.matching.exception.MatchParticipantMismatchException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 4, 27, 10, 0);
    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("start: 매칭 생성")
    class Start {

        @Test
        @DisplayName("두 userId 중 작은 쪽이 userA, 큰 쪽이 userB로 정렬된다")
        void start_sortsUserIds() {
            // when
            final Match match = Match.start(5L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(match.getUserAId()).isEqualTo(2L);
            assertThat(match.getUserBId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("입력 순서와 무관하게 동일하게 정렬된다")
        void start_resultsSameRegardlessOfArgumentOrder() {
            // when
            final Match a = Match.start(2L, 5L, ROOM_ID, STARTED_AT);
            final Match b = Match.start(5L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(a.getUserAId()).isEqualTo(b.getUserAId());
            assertThat(a.getUserBId()).isEqualTo(b.getUserBId());
        }

        @Test
        @DisplayName("초기 상태는 STARTED, endedAt은 null이다")
        void start_initialStateIsStarted() {
            // when
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);

            // then
            assertThat(match.getStatus()).isEqualTo(MatchStatus.STARTED);
            assertThat(match.getStartedAt()).isEqualTo(STARTED_AT);
            assertThat(match.getEndedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("end: 통화 종료")
    class End {

        @Test
        @DisplayName("STARTED 상태에서 end를 호출하면 ENDED로 전이되고 endedAt이 설정된다")
        void end_whenStarted_transitionsToEnded() {
            // given
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);
            final LocalDateTime endedAt = STARTED_AT.plusMinutes(5);

            // when
            match.end(endedAt);

            // then
            assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
            assertThat(match.getEndedAt()).isEqualTo(endedAt);
        }

        @Test
        @DisplayName("이미 ENDED 상태에서 end를 호출해도 endedAt이 변하지 않는다 (멱등)")
        void end_whenAlreadyEnded_isIdempotent() {
            // given
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);
            final LocalDateTime firstEnd = STARTED_AT.plusMinutes(5);
            final LocalDateTime secondEnd = STARTED_AT.plusMinutes(10);
            match.end(firstEnd);

            // when
            match.end(secondEnd);

            // then
            assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
            assertThat(match.getEndedAt()).isEqualTo(firstEnd);
        }
    }

    @Nested
    @DisplayName("involves / counterpartOf / caller / callee")
    class Participation {

        @Test
        @DisplayName("involves는 참여자에 대해 true, 비참여자에 대해 false를 반환한다")
        void involves_returnsTrueForParticipant() {
            // given
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(match.involves(1L)).isTrue();
            assertThat(match.involves(2L)).isTrue();
            assertThat(match.involves(99L)).isFalse();
        }

        @Test
        @DisplayName("counterpartOf는 상대 userId를 반환한다")
        void counterpartOf_returnsOther() {
            // given
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(match.counterpartOf(1L)).isEqualTo(2L);
            assertThat(match.counterpartOf(2L)).isEqualTo(1L);
        }

        @Test
        @DisplayName("counterpartOf는 비참여자에 대해 MatchParticipantMismatchException을 던진다")
        void counterpartOf_whenNotParticipant_throws() {
            // given
            final Match match = Match.start(1L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThatThrownBy(() -> match.counterpartOf(99L))
                    .isInstanceOf(MatchParticipantMismatchException.class);
        }

        @Test
        @DisplayName("callerUserId는 userA(작은 쪽), calleeUserId는 userB(큰 쪽)을 반환한다")
        void callerAndCallee_followIdOrdering() {
            // given
            final Match match = Match.start(5L, 2L, ROOM_ID, STARTED_AT);

            // when & then
            assertThat(match.callerUserId()).isEqualTo(2L);
            assertThat(match.calleeUserId()).isEqualTo(5L);
        }
    }
}
