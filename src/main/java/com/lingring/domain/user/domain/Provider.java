package com.lingring.domain.user.domain;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Arrays;

public enum Provider {

    KAKAO,
    APPLE,
    GOOGLE;

    public static Provider from(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT_VALUE, "provider가 비어있습니다.");
        }
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.NOT_SUPPORTED,
                        "지원하지 않는 provider입니다: %s".formatted(raw)
                ));
    }
}