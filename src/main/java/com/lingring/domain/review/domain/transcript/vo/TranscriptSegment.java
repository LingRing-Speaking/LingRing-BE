package com.lingring.domain.review.domain.transcript.vo;

public record TranscriptSegment(
        Long userId,
        Double startSec,
        Double endSec,
        String text
) {
}
