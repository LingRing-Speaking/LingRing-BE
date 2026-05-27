package com.lingring.domain.user.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.user.domain.vo.WithdrawDescription;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "withdrawal_log")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class WithdrawalLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private WithdrawReason reason;

    @Embedded
    private WithdrawDescription description;

    private WithdrawalLog(
            @NonNull final WithdrawReason reason,
            final WithdrawDescription description
    ) {
        this.reason = reason;
        this.description = description;
    }

    public static WithdrawalLog record(
            @NonNull final WithdrawReason reason,
            final String description
    ) {
        if (reason != WithdrawReason.OTHER) {
            return new WithdrawalLog(reason, null);
        }
        return new WithdrawalLog(reason, new WithdrawDescription(description));
    }
}
