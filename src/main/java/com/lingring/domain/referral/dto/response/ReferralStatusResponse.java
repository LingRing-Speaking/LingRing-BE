package com.lingring.domain.referral.dto.response;

import java.time.LocalDateTime;

public record ReferralStatusResponse(
        boolean redeemable,
        LocalDateTime redeemableUntil
) {
}
