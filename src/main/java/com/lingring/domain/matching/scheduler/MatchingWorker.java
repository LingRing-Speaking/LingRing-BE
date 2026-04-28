package com.lingring.domain.matching.scheduler;

import com.lingring.domain.matching.service.MatchingExecutor;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingWorker {

    private final MatchingExecutor matchingExecutor;

    @Scheduled(fixedDelay = 3000L)
    @SchedulerLock(
            name = "matching-worker",
            lockAtMostFor = "PT10S",
            lockAtLeastFor = "PT1S"
    )
    public void runOnce() {
        matchingExecutor.executeRound();
    }
}
