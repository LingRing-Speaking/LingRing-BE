package com.lingring.domain.user.domain;

public record PresignedUploadUrl(
        String uploadUrl,
        String key
) {
}
