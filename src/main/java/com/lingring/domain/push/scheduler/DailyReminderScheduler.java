package com.lingring.domain.push.scheduler;

import com.lingring.domain.push.service.PushSendService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyReminderScheduler {

    private final PushSendService pushSendService;

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    @SchedulerLock(
            name = "daily-reminder",
            lockAtMostFor = "PT55M",
            lockAtLeastFor = "PT1M"
    )
    public void run() {
        pushSendService.sendDailyReminder();
    }
}
