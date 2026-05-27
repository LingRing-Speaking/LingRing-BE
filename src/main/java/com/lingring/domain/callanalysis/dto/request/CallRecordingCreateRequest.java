package com.lingring.domain.callanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CallRecordingCreateRequest(
        @NotBlank String recordingKey
) {
}
