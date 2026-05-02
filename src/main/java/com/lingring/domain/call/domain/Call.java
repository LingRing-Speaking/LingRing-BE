package com.lingring.domain.call.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "calls",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calls_room_id",
                columnNames = "room_id"
        ),
        indexes = {
                @Index(name = "idx_calls_user_a_id", columnList = "user_a_id"),
                @Index(name = "idx_calls_user_b_id", columnList = "user_b_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Call extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "room_id", nullable = false, length = 36)
    private UUID roomId;

    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    private Call(
            @NonNull final UUID roomId,
            @NonNull final Long userAId,
            @NonNull final Long userBId,
            @NonNull final LocalDateTime startedAt
    ) {
        this.roomId = roomId;
        this.userAId = userAId;
        this.userBId = userBId;
        this.startedAt = startedAt;
    }

    public static Call start(
            @NonNull final Long firstUserId,
            @NonNull final Long secondUserId,
            @NonNull final UUID roomId,
            @NonNull final LocalDateTime startedAt
    ) {
        final Long userAId = Math.min(firstUserId, secondUserId);
        final Long userBId = Math.max(firstUserId, secondUserId);
        return new Call(roomId, userAId, userBId, startedAt);
    }

    public void end(@NonNull final LocalDateTime endedAt) {
        if (!isActive()) {
            return;
        }
        this.endedAt = endedAt;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public boolean involves(@NonNull final Long userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    public Long callerUserId() {
        return userAId;
    }

    public Long calleeUserId() {
        return userBId;
    }

    public Long counterpartOf(@NonNull final Long userId) {
        if (userAId.equals(userId)) {
            return userBId;
        }
        if (userBId.equals(userId)) {
            return userAId;
        }
        throw new CallParticipantMismatchException(roomId, userId);
    }
}
