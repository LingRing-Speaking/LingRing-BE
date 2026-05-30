package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.analysis.vo.MistakeItem;

public record MistakeItemResponse(
        String tag,
        String wrong,
        String improved,
        String reason,
        String koMeaning
) {

    public static MistakeItemResponse from(final MistakeItem item) {
        return new MistakeItemResponse(
                item.tag().name(),
                item.wrong(),
                item.improved(),
                item.reason(),
                item.koMeaning()
        );
    }
}
