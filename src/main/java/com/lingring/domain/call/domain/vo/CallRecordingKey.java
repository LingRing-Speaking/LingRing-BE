package com.lingring.domain.call.domain.vo;

import com.lingring.domain.call.exception.CallRecordingKeyForbiddenException;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class CallRecordingKey {

    private static final String PREFIX = "call-recordings/";

    private final String value;

    private CallRecordingKey(final String value) {
        this.value = value;
    }

    public static CallRecordingKey from(@NonNull final String value) {
        return new CallRecordingKey(value);
    }

    public static CallRecordingKey generateFor(
            @NonNull final Long callId,
            @NonNull final Long userId
    ) {
        return new CallRecordingKey(PREFIX + callId + "/" + userId + "/" + UUID.randomUUID());
    }

    public void requireOwnedBy(
            @NonNull final Long callId,
            @NonNull final Long userId
    ) {
        final String requiredPrefix = PREFIX + callId + "/" + userId + "/";
        if (!value.startsWith(requiredPrefix)) {
            throw new CallRecordingKeyForbiddenException(value);
        }
    }
}
