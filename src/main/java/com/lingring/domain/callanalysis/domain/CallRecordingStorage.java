package com.lingring.domain.callanalysis.domain;

public interface CallRecordingStorage {

    PresignedUpload generateUploadUrl(String key, String contentType, long contentLength);

    boolean exists(String key);

    String contentTypeOf(String key);
}
