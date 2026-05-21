package com.lingring.domain.call.domain.vo;

import java.util.List;
import java.util.Objects;

public record TranscriptContent(List<TranscriptSegment> segments) {

    public TranscriptContent {
        Objects.requireNonNull(segments, "segments must not be null");
        segments = List.copyOf(segments);
    }
}
