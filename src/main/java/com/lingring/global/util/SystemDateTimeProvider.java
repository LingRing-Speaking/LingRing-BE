package com.lingring.global.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SystemDateTimeProvider implements DateTimeProvider {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(Zones.SEOUL).truncatedTo(ChronoUnit.MICROS);
    }
}