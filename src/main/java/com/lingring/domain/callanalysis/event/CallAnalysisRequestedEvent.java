package com.lingring.domain.callanalysis.event;

import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.List;

public record CallAnalysisRequestedEvent(
        Long callId,
        List<RecordingReference> recordings
) {
}
