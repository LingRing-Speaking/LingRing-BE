package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class DisconnectSchedulerTest {

    private static final Duration GRACE = Duration.ofMillis(200);

    private ThreadPoolTaskScheduler taskScheduler;
    private DisconnectScheduler scheduler;

    @BeforeEach
    void setUp() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("disconnect-test-");
        taskScheduler.initialize();
        scheduler = new DisconnectScheduler(taskScheduler, GRACE);
    }

    @AfterEach
    void tearDown() {
        taskScheduler.shutdown();
    }

    @Nested
    @DisplayName("schedule: grace period 후 작업 실행")
    class Schedule {

        @Test
        @DisplayName("grace period가 지나면 예약된 작업이 실행된다")
        void schedule_runsTaskAfterGracePeriod() {
            // given
            final AtomicInteger executed = new AtomicInteger();

            // when
            scheduler.schedule(1L, executed::incrementAndGet);

            // then
            Awaitility.await().atMost(Duration.ofSeconds(2))
                    .untilAtomic(executed, org.hamcrest.Matchers.equalTo(1));
        }

        @Test
        @DisplayName("같은 userId로 다시 schedule하면 기존 작업은 취소되고 새 작업만 실행된다")
        void schedule_whenRescheduled_replacesPreviousTask() {
            // given
            final AtomicInteger first = new AtomicInteger();
            final AtomicInteger second = new AtomicInteger();

            // when
            scheduler.schedule(1L, first::incrementAndGet);
            scheduler.schedule(1L, second::incrementAndGet);

            // then
            Awaitility.await().atMost(Duration.ofSeconds(2))
                    .untilAtomic(second, org.hamcrest.Matchers.equalTo(1));
            assertThat(first.get()).isZero();
        }
    }

    @Nested
    @DisplayName("cancel: 예약된 작업 취소")
    class Cancel {

        @Test
        @DisplayName("grace period 도래 전에 cancel하면 작업이 실행되지 않는다")
        void cancel_beforeFiring_preventsExecution() throws Exception {
            // given
            final AtomicInteger executed = new AtomicInteger();
            scheduler.schedule(1L, executed::incrementAndGet);

            // when
            scheduler.cancel(1L);
            Thread.sleep(GRACE.plusMillis(200).toMillis());

            // then
            assertThat(executed.get()).isZero();
        }

        @Test
        @DisplayName("등록된 작업이 없는 userId를 cancel해도 예외가 발생하지 않는다")
        void cancel_whenNoPendingTask_doesNothing() {
            // when & then
            scheduler.cancel(99L);
        }
    }
}
