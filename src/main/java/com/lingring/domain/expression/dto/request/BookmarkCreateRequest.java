package com.lingring.domain.expression.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "source")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BookmarkCreateRequest.AnalysisMistake.class, name = "ANALYSIS_MISTAKE"),
        @JsonSubTypes.Type(value = BookmarkCreateRequest.DailyExpression.class, name = "DAILY_EXPRESSION"),
        @JsonSubTypes.Type(value = BookmarkCreateRequest.Icebreaker.class, name = "ICEBREAKER")
})
public sealed interface BookmarkCreateRequest {

    record AnalysisMistake(
            @NotNull(message = "analysisId는 필수입니다.")
            Long analysisId,
            @NotNull(message = "mistakeId는 필수입니다.")
            @PositiveOrZero(message = "mistakeId는 0 이상이어야 합니다.")
            Integer mistakeId
    ) implements BookmarkCreateRequest {
    }

    record DailyExpression(
            @NotNull(message = "recommendedExpressionId는 필수입니다.")
            Long recommendedExpressionId
    ) implements BookmarkCreateRequest {
    }

    record Icebreaker(
            @NotNull(message = "icebreakerId는 필수입니다.")
            Long icebreakerId
    ) implements BookmarkCreateRequest {
    }
}
