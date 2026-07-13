package com.lingring.domain.expression.domain;

/**
 * 찜(북마크)한 표현의 출처. 새 소스가 생기면 여기와 {@code BookmarkCreateRequest}의
 * 서브타입에 함께 추가한다.
 */
public enum BookmarkSource {

    /** 통화 분석 결과의 "이렇게 말해보세요"(mistake) 항목. */
    ANALYSIS_MISTAKE,

    /** 메인 화면의 오늘의 추천 표현. */
    DAILY_EXPRESSION,

    /** 매칭 화면의 아이스브레이커. */
    ICEBREAKER
}
