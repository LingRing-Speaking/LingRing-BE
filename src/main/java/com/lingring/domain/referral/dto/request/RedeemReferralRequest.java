package com.lingring.domain.referral.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RedeemReferralRequest(
        @NotBlank
        String nickname
) {
}
