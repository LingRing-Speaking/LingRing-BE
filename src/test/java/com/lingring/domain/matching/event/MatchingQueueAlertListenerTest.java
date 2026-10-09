package com.lingring.domain.matching.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.infrastructure.slack.FakeMatchingAlertSender;
import com.lingring.infrastructure.slack.FakeMatchingAlertSender.SentQueueEnteredAlert;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MatchingQueueAlertListenerTest {

    private static final Long USER_ID = 123L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 10, 9, 14, 2, 11);

    private FakeMatchingAlertSender matchingAlertSender;
    private MatchingQueueAlertListener listener;

    @BeforeEach
    void setUp() {
        matchingAlertSender = new FakeMatchingAlertSender();
        listener = new MatchingQueueAlertListener(matchingAlertSender);
    }

    @Nested
    @DisplayName("on: 매칭 대기 진입 알림")
    class On {

        @Test
        @DisplayName("매칭 대기 진입 이벤트면 진입 유저와 시각으로 알림을 보낸다")
        void on_whenMatchingRequested_sendsAlert() {
            // when
            listener.on(UserActionEvent.matchingRequested(USER_ID, OCCURRED_AT));

            // then
            assertThat(matchingAlertSender.sent())
                    .containsExactly(new SentQueueEnteredAlert(USER_ID, OCCURRED_AT));
        }

        @Test
        @DisplayName("매칭 대기 진입이 아닌 이벤트면 알림을 보내지 않는다")
        void on_whenOtherEvent_sendsNothing() {
            // when
            listener.on(UserActionEvent.matchingCancelled(USER_ID, 30_000L, OCCURRED_AT));

            // then
            assertThat(matchingAlertSender.sent()).isEmpty();
        }
    }
}
