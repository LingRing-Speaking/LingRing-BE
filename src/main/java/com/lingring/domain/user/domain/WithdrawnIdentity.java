package com.lingring.domain.user.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.Period;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "withdrawn_identity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_withdrawn_identity_identity_hash",
                columnNames = {"identity_hash"}
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class WithdrawnIdentity {

    public static final Period RETENTION_PERIOD = Period.ofYears(1);

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "identity_hash", nullable = false, length = 64)
    private String identityHash;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    private WithdrawnIdentity(
            @NonNull final String identityHash,
            @NonNull final LocalDateTime withdrawnAt
    ) {
        this.identityHash = identityHash;
        this.withdrawnAt = withdrawnAt;
    }

    public static WithdrawnIdentity record(
            @NonNull final String identityHash,
            @NonNull final LocalDateTime withdrawnAt
    ) {
        return new WithdrawnIdentity(identityHash, withdrawnAt);
    }

    public static LocalDateTime expirationThreshold(@NonNull final LocalDateTime now) {
        return now.minus(RETENTION_PERIOD);
    }

    public void renew(@NonNull final LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
}
