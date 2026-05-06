package com.lingring.domain.useragreement.domain;

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
