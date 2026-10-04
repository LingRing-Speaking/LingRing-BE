package com.lingring.domain.user.scheduler;

import com.lingring.domain.user.service.WithdrawnIdentityPurgeService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawnIdentityPurgeScheduler {

    private final WithdrawnIdentityPurgeService withdrawnIdentityPurgeService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(
            name = "withdrawn-identity-purge",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT1M"
    )
    public void run() {
        withdrawnIdentityPurgeService.purgeExpired();
    }
}
