package com.lingring.domain.callanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CallRecordingPresignedUrlRequest(
        @NotBlank String contentType,
        @Positive long contentLength
) {
}
