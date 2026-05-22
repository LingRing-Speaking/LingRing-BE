package com.lingring.infrastructure.sqs;

import com.lingring.domain.call.domain.vo.TranscriptSegment;
import java.util.List;

public record TranscriptResultMessage(
        Long callId,
        List<TranscriptSegment> segments
) {
}
