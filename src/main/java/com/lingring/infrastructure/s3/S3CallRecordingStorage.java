package com.lingring.infrastructure.s3;

import com.lingring.domain.callanalysis.domain.port.CallRecordingStorage;
import com.lingring.domain.callanalysis.domain.port.PresignedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3CallRecordingStorage implements CallRecordingStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final CallRecordingS3Properties callRecordingProperties;

    @Override
    public PresignedUpload generateUploadUrl(
            final String key,
            final String contentType,
            final long contentLength
    ) {
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(callRecordingProperties.uploadTtl())
                .putObjectRequest(putObjectRequest)
                .build();

        final PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUpload(presigned.url().toString(), key);
    }

    @Override
    public boolean exists(final String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build());
            return true;
        } catch (final NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String contentTypeOf(final String key) {
        final HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build());
        return response.contentType();
    }
}
