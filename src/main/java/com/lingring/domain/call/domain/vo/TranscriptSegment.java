package com.lingring.domain.call.domain.vo;

public record TranscriptSegment(
        Long userId,
        Double startSec,
        Double endSec,
        String text
) {
}
