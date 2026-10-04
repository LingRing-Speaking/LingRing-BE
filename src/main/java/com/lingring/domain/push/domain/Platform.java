package com.lingring.domain.push.domain;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Arrays;

public enum Platform {

    IOS,
    ANDROID;

    public static Platform from(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "platform이 비어있습니다.");
        }
        return Arrays.stream(values())
                .filter(platform -> platform.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.NOT_SUPPORTED,
                        "지원하지 않는 platform입니다: %s".formatted(raw)
                ));
    }
}
