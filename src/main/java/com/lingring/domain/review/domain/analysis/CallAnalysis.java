package com.lingring.domain.review.domain.analysis;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "call_analysis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_analysis_call_id_user_id",
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

    @ColumnDefault("false")
    @Column(name = "requested", nullable = false)
    private boolean requested;

    private CallAnalysis(
            @NonNull final Long callId,
            @NonNull final Long userId,
            @NonNull final CallAnalysisStatus status
    ) {
        this.callId = callId;
        this.userId = userId;
        this.status = status;
        this.requested = false;
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

    public boolean markRequestedIfAbsent() {
        if (this.requested) {
            return false;
        }
        this.requested = true;
        return true;
    }

    public boolean isCompleted() {
        return status == CallAnalysisStatus.COMPLETED;
    }

    /**
     * 결과 내 인덱스로 mistake를 조회한다. COMPLETED 결과는 {@link #complete}의
     * 가드로 불변이므로 인덱스가 안정 식별자 역할을 한다 (표현 찜에서 사용).
     */
    public Optional<MistakeItem> findMistake(final int index) {
        if (!isCompleted() || result == null) {
            return Optional.empty();
        }
        final List<MistakeItem> items = result.mistakes().items();
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        return Optional.of(items.get(index));
    }

    public boolean isOwnedBy(@NonNull final Long requesterId) {
        return this.userId.equals(requesterId);
    }
}
