package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.CallTranscript;
import com.lingring.domain.call.domain.CallTranscriptStatus;
import com.lingring.domain.call.domain.vo.TranscriptSegment;
import java.util.List;

public record CallTranscriptResponse(
        CallTranscriptStatus status,
        List<TranscriptSegment> segments
) {

    public static CallTranscriptResponse from(final CallTranscript transcript) {
        if (transcript.getContent() == null) {
            return new CallTranscriptResponse(transcript.getStatus(), null);
        }
        return new CallTranscriptResponse(
                transcript.getStatus(),
                transcript.getContent().segments()
        );
    }
}
