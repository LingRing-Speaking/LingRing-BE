package com.lingring.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WithdrawReason {

    NO_GOOD_MATCH("매칭이 잘 안 돼요"),
    NO_PROGRESS("영어 실력이 늘지 않아요"),
    BUGGY("앱이 자주 멈춰요 / 오류가 많아요"),
    RARELY_USE("잘 사용하지 않아요"),
    MISSING_FEATURE("원하는 기능이 없어요"),
    OTHER("기타");

    private final String description;
}