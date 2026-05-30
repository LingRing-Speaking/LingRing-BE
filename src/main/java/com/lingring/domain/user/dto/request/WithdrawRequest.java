package com.lingring.domain.user.dto.request;

import com.lingring.domain.user.domain.WithdrawReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WithdrawRequest(
        @NotNull(message = "reason이 비어있습니다.")
        WithdrawReason reason,
        @Size(max = 200, message = "description은 200자 이하여야 합니다.")
        String description
) {

    @AssertTrue(message = "OTHER 사유일 때 description은 필수입니다.")
    public boolean isDescriptionPresentWhenOther() {
        if (reason != WithdrawReason.OTHER) {
            return true;
        }
        return description != null && !description.isBlank();
    }
}