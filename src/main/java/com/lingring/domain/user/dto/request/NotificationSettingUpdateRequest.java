package com.lingring.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record NotificationSettingUpdateRequest(
        @NotNull
        Boolean marketingPush
) {
}
