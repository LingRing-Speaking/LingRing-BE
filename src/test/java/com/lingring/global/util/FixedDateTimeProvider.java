package com.lingring.global.util;

import java.time.LocalDateTime;

public final class FixedDateTimeProvider implements DateTimeProvider {

    private LocalDateTime fixedTime;

    public FixedDateTimeProvider(final LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }

    public void setFixedTime(final LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public LocalDateTime now() {
        return fixedTime;
    }
}
