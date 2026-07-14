package com.lingring.domain.matching.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CallInvitationCreateRequest(
        @NotNull(message = "inviteeUserId가 비어있습니다.")
        @Positive(message = "inviteeUserId는 양수여야 합니다.")
        Long inviteeUserId
) {
}
