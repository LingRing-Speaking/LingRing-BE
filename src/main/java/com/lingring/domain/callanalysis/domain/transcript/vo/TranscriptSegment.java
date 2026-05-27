package com.lingring.domain.callanalysis.domain.transcript.vo;

public record TranscriptSegment(
        Long userId,
        Double startSec,
        Double endSec,
        String text
) {
}
