package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.transcript.vo.TranscriptSegment;

public record TranscriptSegmentResponse(
        Long userId,
        Double startSec,
        Double endSec,
        String text
) {

    public static TranscriptSegmentResponse from(final TranscriptSegment segment) {
        return new TranscriptSegmentResponse(
                segment.userId(),
                segment.startSec(),
                segment.endSec(),
                segment.text()
        );
    }
}
