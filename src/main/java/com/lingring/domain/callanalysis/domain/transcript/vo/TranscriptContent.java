package com.lingring.domain.callanalysis.domain.transcript.vo;

import java.util.List;
import java.util.Objects;

public record TranscriptContent(List<TranscriptSegment> segments) {

    public TranscriptContent {
        Objects.requireNonNull(segments, "segments must not be null");
        segments = List.copyOf(segments);
    }
}
