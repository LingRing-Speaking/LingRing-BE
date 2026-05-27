package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.callanalysis.domain.port.PresignedUpload;

public record CallRecordingPresignedUrlResponse(
        String url,
        String key
) {

    public static CallRecordingPresignedUrlResponse from(final PresignedUpload presigned) {
        return new CallRecordingPresignedUrlResponse(presigned.url(), presigned.key());
    }
}
