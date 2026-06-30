package com.lingring.domain.review.domain.quota;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.review.exception.AnalysisQuotaExhaustedException;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "analysis_quota",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_analysis_quota_user_id",
                columnNames = {"user_id"}
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class AnalysisQuota extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    private FreeTicket freeTicket;

    @Embedded
    private PaidTicket paidTicket;

    private AnalysisQuota(@NonNull final Long userId) {
        this.userId = userId;
        this.freeTicket = FreeTicket.initial();
        this.paidTicket = PaidTicket.empty();
    }

    public static AnalysisQuota initial(@NonNull final Long userId) {
        return new AnalysisQuota(userId);
    }

    public void consumeOn(final LocalDate today) {
        if (freeTicket.consumeOn(today)) {
            return;
        }
        if (paidTicket.consume()) {
            return;
        }
        throw new AnalysisQuotaExhaustedException(userId);
    }

    public void charge(final int amount) {
        paidTicket.charge(amount);
    }

    public int freeRemainingOn(final LocalDate today) {
        return freeTicket.remainingOn(today);
    }

    public int paidRemaining() {
        return paidTicket.getCount();
    }

    public LocalDate freeResetDate() {
        return freeTicket.getResetDate();
    }
}
