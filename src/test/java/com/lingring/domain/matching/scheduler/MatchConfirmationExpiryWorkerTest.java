package com.lingring.domain.matching.scheduler;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;

import com.lingring.domain.matching.service.MatchingService;
import java.time.Duration;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MatchConfirmationExpiryWorkerTest {

    private final MatchingService matchingService = Mockito.mock(MatchingService.class);
    private final MatchConfirmationExpiryWorker worker = new MatchConfirmationExpiryWorker(matchingService);

    @Test
    @DisplayName("runOnce 호출 시 MatchingService.expireOverdueConfirmations 위임")
    void runOnce_delegates() {
        // when
        worker.runOnce();

        // then
        then(matchingService).should().expireOverdueConfirmations();
    }

    @Test
    @DisplayName("@SchedulerLock의 lockAtMostFor / lockAtLeastFor는 ShedLock이 파싱 가능한 표기여야 한다")
    void schedulerLockDurations_areParseable() throws NoSuchMethodException {
        // given
        final SchedulerLock annotation = MatchConfirmationExpiryWorker.class
                .getMethod("runOnce")
                .getAnnotation(SchedulerLock.class);

        // when & then
        assertThatNoException()
                .isThrownBy(() -> parseLikeShedLock(annotation.lockAtMostFor()));
        assertThatNoException()
                .isThrownBy(() -> parseLikeShedLock(annotation.lockAtLeastFor()));
    }

    private static Duration parseLikeShedLock(final String value) {
        try {
            return Duration.ofMillis(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return Duration.parse(value);
        }
    }
}
