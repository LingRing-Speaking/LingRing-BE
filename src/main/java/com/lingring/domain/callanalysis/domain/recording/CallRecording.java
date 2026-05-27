package com.lingring.domain.callanalysis.domain.recording;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "call_recordings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_recordings_call_user",
                columnNames = {"call_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_call_recordings_call_id", columnList = "call_id"),
                @Index(name = "idx_call_recordings_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class CallRecording extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "call_id", nullable = false)
    private Long callId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recording_key", nullable = false, length = 512)
    private String recordingKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CallRecordingStatus status;

    private CallRecording(
            @NonNull final Long callId,
            @NonNull final Long userId,
            @NonNull final String recordingKey,
            @NonNull final String contentType,
            @NonNull final CallRecordingStatus status
    ) {
        this.callId = callId;
        this.userId = userId;
        this.recordingKey = recordingKey;
        this.contentType = contentType;
        this.status = status;
    }

    public static CallRecording upload(
            @NonNull final Long callId,
            @NonNull final Long userId,
            @NonNull final String recordingKey,
            @NonNull final String contentType
    ) {
        return new CallRecording(callId, userId, recordingKey, contentType, CallRecordingStatus.UPLOADED);
    }
}
