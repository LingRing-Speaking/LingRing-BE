package com.lingring.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SystemDateTimeProvider implements DateTimeProvider {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(SEOUL_ZONE).truncatedTo(ChronoUnit.MICROS);
    }
}
