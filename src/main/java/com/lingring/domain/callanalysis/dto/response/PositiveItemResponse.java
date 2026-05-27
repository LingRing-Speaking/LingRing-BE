package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.callanalysis.domain.analysis.vo.PositiveItem;

public record PositiveItemResponse(
        String sentence,
        String goodPart,
        String koMeaning
) {

    public static PositiveItemResponse from(final PositiveItem item) {
        return new PositiveItemResponse(
                item.sentence(),
                item.goodPart(),
                item.koMeaning()
        );
    }
}
