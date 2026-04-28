package com.lingring.domain.icebreaker.dto.response;

import com.lingring.domain.icebreaker.domain.Icebreaker;
import java.time.LocalDateTime;

public record IcebreakerResponse(
        Long id,
        String expression,
        String meaning,
        LocalDateTime createdAt
) {

    public static IcebreakerResponse from(final Icebreaker icebreaker) {
        return new IcebreakerResponse(
                icebreaker.getId(),
                icebreaker.getExpression().getValue(),
                icebreaker.getMeaning().getValue(),
                icebreaker.getCreatedAt()
        );
    }
}
