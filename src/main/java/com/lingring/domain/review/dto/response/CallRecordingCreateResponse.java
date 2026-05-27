package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.recording.CallRecording;
import com.lingring.domain.review.domain.recording.CallRecordingStatus;

public record CallRecordingCreateResponse(
        Long recordingId,
        CallRecordingStatus status
) {

    public static CallRecordingCreateResponse from(final CallRecording recording) {
        return new CallRecordingCreateResponse(recording.getId(), recording.getStatus());
    }
}
