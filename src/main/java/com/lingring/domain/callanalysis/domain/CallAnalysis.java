package com.lingring.domain.callanalysis.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.callanalysis.domain.vo.AnalysisResult;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "call_analyses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_analyses_call_id_user_id",
                columnNames = {"call_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CallAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "call_id", nullable = false)
    private Long callId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CallAnalysisStatus status;

    @Column(name = "model_identifier", length = 128)
    private String modelIdentifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "JSON")
    private AnalysisResult result;

    private CallAnalysis(
            @NonNull final Long callId,
            @NonNull final Long userId,
            @NonNull final CallAnalysisStatus status
    ) {
        this.callId = callId;
        this.userId = userId;
        this.status = status;
    }

    public static CallAnalysis processing(@NonNull final Long callId, @NonNull final Long userId) {
        return new CallAnalysis(callId, userId, CallAnalysisStatus.PROCESSING);
    }

    public void complete(@NonNull final AnalysisResult newResult, @NonNull final String modelIdentifier) {
        if (status == CallAnalysisStatus.COMPLETED) {
            return;
        }
        this.status = CallAnalysisStatus.COMPLETED;
        this.result = newResult;
        this.modelIdentifier = modelIdentifier;
    }

    public void fail() {
        if (status == CallAnalysisStatus.COMPLETED) {
            return;
        }
        this.status = CallAnalysisStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == CallAnalysisStatus.COMPLETED;
    }
}
