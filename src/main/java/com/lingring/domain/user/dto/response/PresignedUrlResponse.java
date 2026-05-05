package com.lingring.domain.user.dto.response;

import com.lingring.domain.user.service.PresignedUploadUrl;

public record PresignedUrlResponse(
        String uploadUrl,
        String key
) {

    public static PresignedUrlResponse from(final PresignedUploadUrl presigned) {
        return new PresignedUrlResponse(presigned.uploadUrl(), presigned.key());
    }
}
