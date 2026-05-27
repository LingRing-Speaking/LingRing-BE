package com.lingring.domain.callanalysis.domain.recording.policy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "call-recording")
public record CallRecordingPolicy(
        List<String> allowedContentTypes,
        long maxContentLength
) {

    public void requireAcceptable(final String contentType, final long contentLength) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_CALL_RECORDING_CONTENT_TYPE,
                    "허용되지 않은 오디오 형식입니다: %s".formatted(contentType)
            );
        }
        if (contentLength <= 0 || contentLength > maxContentLength) {
            throw new BadRequestException(
                    ErrorCode.CALL_RECORDING_TOO_LARGE,
                    "녹음 파일 크기는 1 ~ %d bytes 사이여야 합니다: %d".formatted(maxContentLength, contentLength)
            );
        }
    }
}
