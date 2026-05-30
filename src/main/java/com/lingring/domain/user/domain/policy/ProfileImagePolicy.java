package com.lingring.domain.user.domain.policy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile-image")
public record ProfileImagePolicy(
        String allowedContentTypePrefix,
        long maxContentLengthBytes
) {

    public void requireAcceptable(final String contentType, final Long contentLength) {
        if (contentType == null || !contentType.startsWith(allowedContentTypePrefix)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_IMAGE_CONTENT_TYPE,
                    "허용된 이미지 형식이 아닙니다: %s".formatted(contentType)
            );
        }
        if (contentLength == null || contentLength <= 0 || contentLength > maxContentLengthBytes) {
            throw new BadRequestException(
                    ErrorCode.IMAGE_TOO_LARGE,
                    "이미지 크기는 1 ~ %d bytes 사이여야 합니다.".formatted(maxContentLengthBytes)
            );
        }
    }
}
