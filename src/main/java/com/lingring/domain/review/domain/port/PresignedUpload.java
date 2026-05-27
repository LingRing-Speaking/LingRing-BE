package com.lingring.domain.review.domain.port;

public record PresignedUpload(
        String url,
        String key
) {
}
