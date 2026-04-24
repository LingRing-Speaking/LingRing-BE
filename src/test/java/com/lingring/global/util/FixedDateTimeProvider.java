package com.lingring.global.util;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class FixedDateTimeProvider implements DateTimeProvider {

    private final LocalDateTime fixedTime;

    @Override
    public LocalDateTime now() {
        return fixedTime;
    }
}
