package com.lingring.domain.review.event;

import com.lingring.domain.review.domain.recording.vo.RecordingReference;
import java.util.List;

public record CallAnalysisRequestedEvent(
        Long callId,
        List<RecordingReference> recordings
) {
}
