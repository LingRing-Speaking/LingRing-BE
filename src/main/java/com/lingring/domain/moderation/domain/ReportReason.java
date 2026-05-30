package com.lingring.domain.moderation.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {

    INAPPROPRIATE_CONVERSATION("부적절한 대화"),
    BAD_MANNERS("비매너 태도"),
    OTHER("기타");

    private final String description;
}
