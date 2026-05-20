package com.lingring.domain.call.domain;

public record PresignedUpload(
        String url,
        String key
) {
}
