package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.CallTranscript;

public record CallTranscriptStartResponse(Long transcriptId) {

    public static CallTranscriptStartResponse from(final CallTranscript transcript) {
        return new CallTranscriptStartResponse(transcript.getId());
    }
}
