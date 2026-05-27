package com.lingring.domain.user.domain.port;

public record PresignedUploadUrl(
        String uploadUrl,
        String key
) {
}
