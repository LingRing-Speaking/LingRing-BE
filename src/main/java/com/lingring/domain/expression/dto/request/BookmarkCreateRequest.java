package com.lingring.domain.expression.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 찜(북마크) 생성 요청. {@code source} 필드로 갈리는 discriminated union이며,
 * 클라이언트는 텍스트를 보내지 않고 서버가 참조된 소스에서 표현/뜻을 채운다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "source")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookmarkCreateRequest.AnalysisMistake.class, name = "ANALYSIS_MISTAKE"),
        @JsonSubTypes.Type(value = BookmarkCreateRequest.DailyExpression.class, name = "DAILY_EXPRESSION"),
        @JsonSubTypes.Type(value = BookmarkCreateRequest.Icebreaker.class, name = "ICEBREAKER")
})
public sealed interface BookmarkCreateRequest {

    /** 통화 분석 결과의 mistake 찜. {@code mistakeId}는 mistakes 리스트 내 인덱스. */
    record AnalysisMistake(
            @NotNull(message = "analysisId는 필수입니다.")
            Long analysisId,
            @NotNull(message = "mistakeId는 필수입니다.")
            @PositiveOrZero(message = "mistakeId는 0 이상이어야 합니다.")
            Integer mistakeId
    ) implements BookmarkCreateRequest {
    }

    /** 오늘의 추천 표현 찜. */
    record DailyExpression(
            @NotNull(message = "recommendedExpressionId는 필수입니다.")
            Long recommendedExpressionId
    ) implements BookmarkCreateRequest {
    }

    /** 아이스브레이커 찜. */
    record Icebreaker(
            @NotNull(message = "icebreakerId는 필수입니다.")
            Long icebreakerId
    ) implements BookmarkCreateRequest {
    }
}
