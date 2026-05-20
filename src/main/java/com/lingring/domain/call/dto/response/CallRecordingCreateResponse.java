package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.domain.CallRecordingStatus;

public record CallRecordingCreateResponse(
        Long recordingId,
        CallRecordingStatus status
) {

    public static CallRecordingCreateResponse from(final CallRecording recording) {
        return new CallRecordingCreateResponse(recording.getId(), recording.getStatus());
    }
}
