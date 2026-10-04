package com.lingring.domain.referral.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "referral_redemption",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_referral_redemption_invitee_id",
                columnNames = {"invitee_id"}
        ),
        indexes = @Index(
                name = "idx_referral_redemption_referrer_id",
                columnList = "referrer_id"
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ReferralRedemption {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;

    // 추천인 탈퇴 시 NULL로 익명화 — 행을 지우면 입력자가 1회 제한을 다시 통과한다
    @Column(name = "referrer_id")
    private Long referrerId;

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;

    private ReferralRedemption(
            @NonNull final Long inviteeId,
            @NonNull final Long referrerId,
            @NonNull final LocalDateTime redeemedAt
    ) {
        this.inviteeId = inviteeId;
        this.referrerId = referrerId;
        this.redeemedAt = redeemedAt;
    }

    public static ReferralRedemption record(
            @NonNull final Long inviteeId,
            @NonNull final Long referrerId,
            @NonNull final LocalDateTime redeemedAt
    ) {
        return new ReferralRedemption(inviteeId, referrerId, redeemedAt);
    }
}
