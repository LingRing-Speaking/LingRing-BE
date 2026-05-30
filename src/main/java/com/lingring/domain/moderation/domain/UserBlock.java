package com.lingring.domain.moderation.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "user_block",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_block_user_blocked",
                columnNames = {"user_id", "blocked_user_id"}
        ),
        indexes = @Index(name = "idx_user_block_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "blocked_user_id", nullable = false)
    private Long blockedUserId;

    private UserBlock(@NonNull final Long userId, @NonNull final Long blockedUserId) {
        this.userId = userId;
        this.blockedUserId = blockedUserId;
    }

    public static UserBlock create(@NonNull final Long userId, @NonNull final Long blockedUserId) {
        return new UserBlock(userId, blockedUserId);
    }
}
