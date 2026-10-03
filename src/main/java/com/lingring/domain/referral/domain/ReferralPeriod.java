package com.lingring.domain.referral.domain;

import java.time.LocalDateTime;
import java.time.Period;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class ReferralPeriod {

    private static final Period REDEEMABLE_PERIOD = Period.ofDays(7);

    private final LocalDateTime until;

    private ReferralPeriod(final LocalDateTime until) {
        this.until = until;
    }

    public static ReferralPeriod startingAt(@NonNull final LocalDateTime joinedAt) {
        return new ReferralPeriod(joinedAt.plus(REDEEMABLE_PERIOD));
    }

    public boolean isOpenAt(@NonNull final LocalDateTime now) {
        return now.isBefore(until);
    }
}
