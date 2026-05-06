package com.lingring.domain.useragreement.domain;

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
}
