package com.lingring.domain.user.domain.vo;

import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true, access = PROTECTED)
public class MarketingPushConsent {

    @Column(name = "marketing_push_agreed", nullable = false)
    private final boolean agreed;

    @Column(name = "marketing_push_updated_at")
    private final LocalDateTime updatedAt;

    private MarketingPushConsent(final boolean agreed, final LocalDateTime updatedAt) {
        this.agreed = agreed;
        this.updatedAt = updatedAt;
    }

    public static MarketingPushConsent none() {
        return new MarketingPushConsent(false, null);
    }

    public MarketingPushConsent change(final boolean agreed, @NonNull final LocalDateTime changedAt) {
        if (this.agreed == agreed) {
            return this;
        }
        return new MarketingPushConsent(agreed, changedAt);
    }
}
