package com.lingring.domain.icebreaker.dto.response;

import com.lingring.domain.icebreaker.domain.Icebreaker;
import java.util.List;

public record IcebreakerListResponse(List<IcebreakerResponse> items) {

    public static IcebreakerListResponse from(final List<Icebreaker> icebreakers) {
        final List<IcebreakerResponse> items = icebreakers.stream()
                .map(IcebreakerResponse::from)
                .toList();
        return new IcebreakerListResponse(items);
    }
}
