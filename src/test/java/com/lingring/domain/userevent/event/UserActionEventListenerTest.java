package com.lingring.domain.userevent.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.userevent.dao.UserEventRepository;
import com.lingring.domain.userevent.domain.EventName;
import com.lingring.domain.userevent.domain.UserEvent;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class UserActionEventListenerTest extends ServiceIntegrationHelper {

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 7, 22, 12, 0, 0);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserEventRepository userEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private List<UserEvent> savedEventsOf(final Long userId) {
        return userEventRepository.findAll().stream()
                .filter(event -> event.getUserId().equals(userId))
                .toList();
    }

    private void awaitSaved(final Long userId) {
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> !savedEventsOf(userId).isEmpty());
    }

    @Nested
    @DisplayName("record: user_event 저장 (비동기 수집)")
    class Record {

        @Test
        @DisplayName("트랜잭션 밖에서 발행된 이벤트도 저장한다 (fallbackExecution — Redis 전용 흐름)")
        void record_whenPublishedWithoutTransaction_savesRow() {
            // given
            final UserActionEvent event = UserActionEvent.matchingCancelled(901L, 30_000L, OCCURRED_AT);

            // when
            eventPublisher.publishEvent(event);

            // then
            awaitSaved(901L);
            final UserEvent saved = savedEventsOf(901L).get(0);
            assertThat(saved.getEventName()).isEqualTo(EventName.MATCHING_CANCELLED);
            assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(saved.getPlatform()).isNull();
            assertThat(saved.getAppVersion()).isNull();
        }

        @Test
        @DisplayName("properties가 JSON으로 저장되고 타입이 유지된 채 읽힌다")
        void record_persistsPropertiesAsJson() {
            // given
            final UserActionEvent event = UserActionEvent.matchingFailed(
                    902L, "CONFIRM_TIMEOUT", 15_000L, true, OCCURRED_AT);

            // when
            eventPublisher.publishEvent(event);

            // then
            awaitSaved(902L);
            final Map<String, Object> properties = savedEventsOf(902L).get(0).getProperties();
            assertThat(properties.get("reason")).isEqualTo("CONFIRM_TIMEOUT");
            assertThat(((Number) properties.get("wait_ms")).longValue()).isEqualTo(15_000L);
            assertThat(properties.get("requeued")).isEqualTo(true);
        }

        @Test
        @DisplayName("properties 없는 이벤트는 properties null로 저장한다")
        void record_whenNoProperties_savesNullProperties() {
            // given
            final UserActionEvent event = UserActionEvent.matchingRequested(903L, OCCURRED_AT);

            // when
            eventPublisher.publishEvent(event);

            // then
            awaitSaved(903L);
            assertThat(savedEventsOf(903L).get(0).getProperties()).isNull();
        }

        @Test
        @DisplayName("트랜잭션 안에서 발행되면 커밋 후 저장한다")
        void record_whenPublishedInTransaction_savesAfterCommit() {
            // given
            final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            // when
            transactionTemplate.executeWithoutResult(status ->
                    eventPublisher.publishEvent(UserActionEvent.matchingRequested(904L, OCCURRED_AT)));

            // then
            awaitSaved(904L);
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면 저장하지 않는다 (유령 이벤트 차단)")
        void record_whenTransactionRolledBack_savesNothing() {
            // given
            final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            // when
            transactionTemplate.executeWithoutResult(status -> {
                eventPublisher.publishEvent(UserActionEvent.matchingRequested(905L, OCCURRED_AT));
                status.setRollbackOnly();
            });

            // then
            Awaitility.await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                    .until(() -> savedEventsOf(905L).isEmpty());
        }
    }
}
