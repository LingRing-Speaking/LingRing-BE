package com.lingring.domain.moderation.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.moderation.domain.vo.ReportDescription;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "user_report",
        indexes = {
                @Index(name = "idx_user_report_user_id", columnList = "user_id"),
                @Index(name = "idx_user_report_reported_user_id", columnList = "reported_user_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Enumerated(STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason;

    @Embedded
    private ReportDescription description;

    private UserReport(
            @NonNull final Long userId,
            @NonNull final Long reportedUserId,
            @NonNull final ReportReason reason,
            @NonNull final ReportDescription description
    ) {
        this.userId = userId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.description = description;
    }

    public static UserReport create(
            @NonNull final Long userId,
            @NonNull final Long reportedUserId,
            @NonNull final ReportReason reason,
            @NonNull final String description
    ) {
        return new UserReport(userId, reportedUserId, reason, new ReportDescription(description));
    }
}
