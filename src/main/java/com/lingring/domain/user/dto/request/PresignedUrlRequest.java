package com.lingring.domain.user.dto.request;

public record PresignedUrlRequest(
        String contentType,
        Long contentLength
) {
}
