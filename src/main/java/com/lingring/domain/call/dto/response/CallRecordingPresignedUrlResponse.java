package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.PresignedUpload;

public record CallRecordingPresignedUrlResponse(
        String url,
        String key
) {

    public static CallRecordingPresignedUrlResponse from(final PresignedUpload presigned) {
        return new CallRecordingPresignedUrlResponse(presigned.url(), presigned.key());
    }
}
