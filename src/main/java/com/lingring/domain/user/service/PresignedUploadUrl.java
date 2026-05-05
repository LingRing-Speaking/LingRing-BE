package com.lingring.domain.user.service;

public record PresignedUploadUrl(
        String uploadUrl,
        String key
) {
}
