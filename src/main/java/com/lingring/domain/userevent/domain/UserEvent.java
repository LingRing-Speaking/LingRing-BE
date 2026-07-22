package com.lingring.domain.userevent.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "user_event",
        indexes = {
                @Index(name = "idx_user_event_event_name_occurred_at", columnList = "event_name, occurred_at"),
                @Index(name = "idx_user_event_user_id_occurred_at", columnList = "user_id, occurred_at")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // FK 없음 — 탈퇴로 users 행이 삭제된 뒤에도 비식별 상태로 보존한다
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(STRING)
    @Column(name = "event_name", nullable = false, length = 40)
    private EventName eventName;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "JSON")
    private Map<String, Object> properties;

    // FE가 X-Platform / X-App-Version 헤더를 보내기 전까지는 항상 null (#195 스코프 외)
    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    private UserEvent(
            @NonNull final Long userId,
            @NonNull final EventName eventName,
            @NonNull final LocalDateTime occurredAt,
            final Map<String, Object> properties
    ) {
        this.userId = userId;
        this.eventName = eventName;
        this.occurredAt = occurredAt;
        this.properties = properties;
    }

    public static UserEvent record(
            final Long userId,
            final EventName eventName,
            final LocalDateTime occurredAt,
            final Map<String, Object> properties
    ) {
        return new UserEvent(userId, eventName, occurredAt, properties);
    }
}
