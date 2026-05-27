package com.lingring.domain.callanalysis.domain.transcript;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.callanalysis.domain.transcript.vo.TranscriptContent;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "call_transcripts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_transcripts_call_id",
                columnNames = "call_id"
        )
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CallTranscript extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "call_id", nullable = false)
    private Long callId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "JSON")
    private TranscriptContent content;

    private CallTranscript(@NonNull final Long callId) {
        this.callId = callId;
    }

    public static CallTranscript create(@NonNull final Long callId) {
        return new CallTranscript(callId);
    }

    public void complete(@NonNull final TranscriptContent newContent) {
        if (content != null) {
            return;
        }
        this.content = newContent;
    }
}
