package com.lingring.domain.matching.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchConfirmationTest {

    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 12, 12, 0, 15);

    @Nested
    @DisplayName("pairKey: 두 userId를 정렬해 만든다")
    class PairKey {

        @Test
        @DisplayName("작은 id가 먼저 오는 pairKey를 반환한다 (입력 순서 무관)")
        void pairKey_isSorted() {
            // when
            final MatchConfirmation c1 = newConfirmation(7L, 42L, 0, 0, DEADLINE);
            final MatchConfirmation c2 = newConfirmation(42L, 7L, 0, 0, DEADLINE);

            // then
            assertThat(c1.pairKey()).isEqualTo("7:42");
            assertThat(c2.pairKey()).isEqualTo("7:42");
        }
    }

    @Nested
    @DisplayName("isExpired: deadline 기준 만료 여부")
    class IsExpired {

        @Test
        @DisplayName("deadline 이전 시각이면 만료되지 않았다")
        void isExpired_beforeDeadline_false() {
            // given
            final MatchConfirmation c = newConfirmation(1L, 2L, 0, 0, DEADLINE);

            // when & then
            assertThat(c.isExpired(DEADLINE.minusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("deadline 이후 시각이면 만료다")
        void isExpired_afterDeadline_true() {
            // given
            final MatchConfirmation c = newConfirmation(1L, 2L, 0, 0, DEADLINE);

            // when & then
            assertThat(c.isExpired(DEADLINE.plusSeconds(1))).isTrue();
        }
    }

    @Nested
    @DisplayName("bothAccepted: 양쪽 수락 여부")
    class BothAccepted {

        @Test
        @DisplayName("두 플래그가 모두 1일 때만 true")
        void bothAccepted_returnsExpected() {
            assertThat(newConfirmation(1L, 2L, 1, 1, DEADLINE).bothAccepted()).isTrue();
            assertThat(newConfirmation(1L, 2L, 1, 0, DEADLINE).bothAccepted()).isFalse();
            assertThat(newConfirmation(1L, 2L, 0, 1, DEADLINE).bothAccepted()).isFalse();
            assertThat(newConfirmation(1L, 2L, 0, 0, DEADLINE).bothAccepted()).isFalse();
        }
    }

    @Nested
    @DisplayName("partnerOf: 주어진 userId의 상대를 반환")
    class PartnerOf {

        @Test
        @DisplayName("userA를 주면 userB를, userB를 주면 userA를 반환한다")
        void partnerOf_returnsOpponent() {
            // given
            final MatchConfirmation c = newConfirmation(7L, 42L, 0, 0, DEADLINE);

            // when & then
            assertThat(c.partnerOf(7L)).isEqualTo(42L);
            assertThat(c.partnerOf(42L)).isEqualTo(7L);
        }
    }

    private MatchConfirmation newConfirmation(
            final Long userA, final Long userB,
            final int userAAccepted, final int userBAccepted,
            final LocalDateTime deadline
    ) {
        final Long a = Math.min(userA, userB);
        final Long b = Math.max(userA, userB);
        return new MatchConfirmation(a, b, userAAccepted == 1, userBAccepted == 1, ROOM_ID, deadline);
    }
}
