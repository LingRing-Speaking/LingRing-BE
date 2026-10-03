package com.lingring.domain.user.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;

import com.lingring.domain.user.service.WithdrawnIdentityPurgeService;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.annotation.Scheduled;

class WithdrawnIdentityPurgeSchedulerTest {

    private final WithdrawnIdentityPurgeService purgeService = Mockito.mock(WithdrawnIdentityPurgeService.class);
    private final WithdrawnIdentityPurgeScheduler scheduler = new WithdrawnIdentityPurgeScheduler(purgeService);

    @Test
    @DisplayName("run 호출 시 WithdrawnIdentityPurgeService.purgeExpired 위임")
    void run_delegates() {
        // when
        scheduler.run();

        // then
        then(purgeService).should().purgeExpired();
    }

    @Test
    @DisplayName("매일 04:00 Asia/Seoul 기준으로 실행된다")
    void run_isScheduledAt4InSeoul() throws NoSuchMethodException {
        // when
        final Scheduled scheduled = WithdrawnIdentityPurgeScheduler.class
                .getMethod("run")
                .getAnnotation(Scheduled.class);

        // then
        assertThat(scheduled.cron()).isEqualTo("0 0 4 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("@SchedulerLock의 lockAtMostFor / lockAtLeastFor는 ShedLock이 파싱 가능한 표기여야 한다")
    void schedulerLockDurations_areParseable() throws NoSuchMethodException {
        // given
        final SchedulerLock annotation = WithdrawnIdentityPurgeScheduler.class
                .getMethod("run")
                .getAnnotation(SchedulerLock.class);

        // when & then
        assertThatNoException().isThrownBy(() -> Duration.parse(annotation.lockAtMostFor()));
        assertThatNoException().isThrownBy(() -> Duration.parse(annotation.lockAtLeastFor()));
    }
}
