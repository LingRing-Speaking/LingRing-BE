package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.CallTranscript;
import com.lingring.domain.call.domain.CallTranscriptStatus;

public record CallTranscriptStartResponse(
        Long transcriptId,
        CallTranscriptStatus status
) {

    public static CallTranscriptStartResponse from(final CallTranscript transcript) {
        return new CallTranscriptStartResponse(transcript.getId(), transcript.getStatus());
    }
}
