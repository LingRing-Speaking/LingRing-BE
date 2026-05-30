package com.lingring.domain.review.domain.port;

import com.lingring.domain.review.domain.port.PresignedUpload;

public interface CallRecordingStorage {

    PresignedUpload generateUploadUrl(String key, String contentType, long contentLength);

    boolean exists(String key);

    String contentTypeOf(String key);
}
