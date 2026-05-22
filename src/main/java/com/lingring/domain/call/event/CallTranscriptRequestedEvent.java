package com.lingring.domain.call.event;

import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.List;

public record CallTranscriptRequestedEvent(
        Long callId,
        List<RecordingReference> recordings
) {
}
