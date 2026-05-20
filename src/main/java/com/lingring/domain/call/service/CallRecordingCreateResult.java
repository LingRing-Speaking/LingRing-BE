package com.lingring.domain.call.service;

import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.dto.response.CallRecordingCreateResponse;

public record CallRecordingCreateResult(
        CallRecordingCreateResponse response,
        boolean created
) {

    public static CallRecordingCreateResult created(final CallRecording recording) {
        return new CallRecordingCreateResult(CallRecordingCreateResponse.from(recording), true);
    }

    public static CallRecordingCreateResult existing(final CallRecording recording) {
        return new CallRecordingCreateResult(CallRecordingCreateResponse.from(recording), false);
    }
}
