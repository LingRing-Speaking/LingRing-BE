package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.transcript.CallTranscript;
import com.lingring.domain.review.domain.transcript.vo.TranscriptSegment;
import java.util.Comparator;
import java.util.List;

public record CallTranscriptResponse(
        Long callId,
        List<TranscriptSegmentResponse> segments
) {

    public static CallTranscriptResponse from(final CallTranscript transcript) {
        final List<TranscriptSegmentResponse> sortedSegments = transcript.getContent().segments().stream()
                .sorted(Comparator.comparingDouble(TranscriptSegment::startSec))
                .map(TranscriptSegmentResponse::from)
                .toList();
        return new CallTranscriptResponse(transcript.getCallId(), sortedSegments);
    }
}
