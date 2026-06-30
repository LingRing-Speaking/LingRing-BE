package com.lingring.domain.user.domain;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum AgreementItem {
    OVER14,
    TERMS,
    PRIVACY,
    VOICE_AI,
    ;

    // 통화 녹음·분석 기능 제공을 위해 VOICE_AI를 포함한 전 항목을 필수 동의로 둔다
    private static final Set<AgreementItem> REQUIRED = EnumSet.allOf(AgreementItem.class);

    public static Set<AgreementItem> required() {
        return EnumSet.copyOf(REQUIRED);
    }

    public static AgreementItem fromString(final String value) {
        final String normalized = value
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
        return Arrays.stream(values())
                .filter(item -> item.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "지원하지 않는 약관 항목입니다: %s".formatted(value)
                ));
    }
}
