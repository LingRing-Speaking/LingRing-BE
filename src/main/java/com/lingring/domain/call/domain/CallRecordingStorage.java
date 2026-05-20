package com.lingring.domain.call.domain;

public interface CallRecordingStorage {

    PresignedUpload generateUploadUrl(String key, String contentType, long contentLength);

    boolean exists(String key);

    String contentTypeOf(String key);
}
