package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.callanalysis.domain.recording.CallRecording;
import com.lingring.domain.callanalysis.domain.recording.CallRecordingStatus;

public record CallRecordingCreateResponse(
        Long recordingId,
        CallRecordingStatus status
) {

    public static CallRecordingCreateResponse from(final CallRecording recording) {
        return new CallRecordingCreateResponse(recording.getId(), recording.getStatus());
    }
}
