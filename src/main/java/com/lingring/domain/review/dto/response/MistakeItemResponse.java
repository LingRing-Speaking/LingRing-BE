package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.analysis.vo.MistakeItem;

public record MistakeItemResponse(
        /** 결과 내 인덱스. COMPLETED 결과는 불변이므로 안정 식별자이며, 찜 요청의 mistakeId로 쓴다. */
        int id,
        String tag,
        String wrong,
        String improved,
        String reason,
        String koMeaning,
        /** 호출자가 찜(저장)했으면 해당 저장 표현 row id, 아니면 null. */
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
