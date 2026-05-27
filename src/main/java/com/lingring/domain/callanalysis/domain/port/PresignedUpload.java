package com.lingring.domain.callanalysis.domain.port;

public record PresignedUpload(
        String url,
        String key
) {
}
