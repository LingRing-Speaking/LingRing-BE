package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.callanalysis.domain.CallRecording;
import com.lingring.domain.callanalysis.domain.CallRecordingStatus;

public record CallRecordingCreateResponse(
        Long recordingId,
        CallRecordingStatus status
) {

    public static CallRecordingCreateResponse from(final CallRecording recording) {
        return new CallRecordingCreateResponse(recording.getId(), recording.getStatus());
    }
}
