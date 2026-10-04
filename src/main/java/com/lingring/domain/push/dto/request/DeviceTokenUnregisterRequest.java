package com.lingring.domain.push.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenUnregisterRequest(
        @NotBlank
        String token
) {
}
