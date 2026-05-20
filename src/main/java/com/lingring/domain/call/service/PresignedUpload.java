package com.lingring.domain.call.service;

public record PresignedUpload(
        String url,
        String key
) {
}
