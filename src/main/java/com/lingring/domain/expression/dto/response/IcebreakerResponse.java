package com.lingring.domain.expression.dto.response;

import com.lingring.domain.expression.domain.Icebreaker;
import java.time.LocalDateTime;

public record IcebreakerResponse(
        Long id,
        String expression,
        String meaning,
        LocalDateTime createdAt,
        /** 호출자가 찜(저장)했으면 해당 저장 표현 row id, 아니면 null. */
        Long bookmarkId
) {

    public static IcebreakerResponse from(final Icebreaker icebreaker, final Long bookmarkId) {
        return new IcebreakerResponse(
                icebreaker.getId(),
                icebreaker.getExpression().getValue(),
                icebreaker.getMeaning().getValue(),
                icebreaker.getCreatedAt(),
                bookmarkId
        );
    }
}
