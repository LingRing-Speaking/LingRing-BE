package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.analysis.vo.MistakeItem;

public record MistakeItemResponse(
        int id,
        String tag,
        String wrong,
        String improved,
        String reason,
        String koMeaning,
        Long bookmarkId
) {

    public static MistakeItemResponse from(final MistakeItem item, final int index, final Long bookmarkId) {
        return new MistakeItemResponse(
                index,
                item.tag().name(),
                item.wrong(),
                item.improved(),
                item.reason(),
                item.koMeaning(),
                bookmarkId
        );
    }
}
