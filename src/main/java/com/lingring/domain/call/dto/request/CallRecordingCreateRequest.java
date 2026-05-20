package com.lingring.domain.call.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CallRecordingCreateRequest(
        @NotBlank String recordingKey
) {
}
