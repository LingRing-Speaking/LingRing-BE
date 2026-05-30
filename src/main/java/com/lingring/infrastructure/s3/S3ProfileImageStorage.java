package com.lingring.infrastructure.s3;

import com.lingring.domain.user.domain.port.PresignedUploadUrl;
import com.lingring.domain.user.domain.port.ProfileImageStorage;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3ProfileImageStorage implements ProfileImageStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    @Override
    public PresignedUploadUrl generateUploadUrl(
            final String key,
            final String contentType,
            final long contentLength
    ) {
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.presignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        final PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUploadUrl(presigned.url().toString(), key);
    }

    @Override
    public boolean exists(final String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
            return true;
        } catch (final NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String publicUrl(final String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.bucket(), properties.region(), key
        );
    }

    @Override
    public void delete(final String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build());
    }
}
