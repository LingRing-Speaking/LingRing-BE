package com.lingring.domain.push.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "device_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_token_token",
                        columnNames = {"token"}
                )
        },
        indexes = {
                @Index(name = "idx_device_token_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class DeviceToken extends BaseTimeEntity {

    public static final int MAX_TOKEN_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = MAX_TOKEN_LENGTH)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    private DeviceToken(
            @NonNull final Long userId,
            @NonNull final String token,
            @NonNull final Platform platform
    ) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public static DeviceToken register(
            @NonNull final Long userId,
            @NonNull final String token,
            @NonNull final Platform platform
    ) {
        return new DeviceToken(userId, token, platform);
    }

    public void reassign(@NonNull final Long userId, @NonNull final Platform platform) {
        this.userId = userId;
        this.platform = platform;
    }
}
