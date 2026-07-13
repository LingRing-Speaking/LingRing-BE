package com.lingring.domain.expression.dto.response;

import com.lingring.domain.expression.domain.Icebreaker;
import java.util.List;
import java.util.Map;

public record IcebreakerListResponse(List<IcebreakerResponse> items) {

    public static IcebreakerListResponse from(
            final List<Icebreaker> icebreakers,
            final Map<Long, Long> bookmarkIdByIcebreakerId
    ) {
        final List<IcebreakerResponse> items = icebreakers.stream()
                .map(icebreaker -> IcebreakerResponse.from(
                        icebreaker, bookmarkIdByIcebreakerId.get(icebreaker.getId())))
                .toList();
        return new IcebreakerListResponse(items);
    }
}
