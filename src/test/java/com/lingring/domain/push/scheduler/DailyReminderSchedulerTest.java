package com.lingring.domain.push.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;

import com.lingring.domain.push.service.PushSendService;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.annotation.Scheduled;

class DailyReminderSchedulerTest {

    private final PushSendService pushSendService = Mockito.mock(PushSendService.class);
    private final DailyReminderScheduler scheduler = new DailyReminderScheduler(pushSendService);

    @Test
    @DisplayName("run 호출 시 PushSendService.sendDailyReminder 위임")
    void run_delegates() {
        // when
        scheduler.run();

        // then
        then(pushSendService).should().sendDailyReminder();
    }

    @Test
    @DisplayName("매일 20:00 Asia/Seoul 기준으로 실행된다")
    void run_isScheduledAt20InSeoul() throws NoSuchMethodException {
        // when
        final Scheduled scheduled = DailyReminderScheduler.class
                .getMethod("run")
                .getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled.cron()).isEqualTo("0 0 20 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("@SchedulerLock의 lockAtMostFor / lockAtLeastFor는 ShedLock이 파싱 가능한 표기여야 한다")
    void schedulerLockDurations_areParseable() throws NoSuchMethodException {
        // given
        final SchedulerLock annotation = DailyReminderScheduler.class
                .getMethod("run")
                .getAnnotation(SchedulerLock.class);

        // when & then
        assertThatNoException().isThrownBy(() -> Duration.parse(annotation.lockAtMostFor()));
        assertThatNoException().isThrownBy(() -> Duration.parse(annotation.lockAtLeastFor()));
    }
}
