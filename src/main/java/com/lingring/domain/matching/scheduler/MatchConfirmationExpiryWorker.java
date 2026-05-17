package com.lingring.domain.matching.scheduler;

import com.lingring.domain.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchConfirmationExpiryWorker {

    private final MatchingService matchingService;

    @Scheduled(fixedDelay = 1000L)
    @SchedulerLock(
            name = "match-confirmation-expiry-worker",
            lockAtMostFor = "PT5S",
            lockAtLeastFor = "PT500MS"
    )
    public void runOnce() {
        matchingService.expireOverdueConfirmations();
    }
}
