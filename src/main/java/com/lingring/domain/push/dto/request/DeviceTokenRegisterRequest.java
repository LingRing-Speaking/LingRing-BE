package com.lingring.domain.push.dto.request;

import com.lingring.domain.push.domain.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenRegisterRequest(
        @NotBlank
        @Size(max = DeviceToken.MAX_TOKEN_LENGTH)
        String token,

        @NotBlank
        String platform
) {
}
