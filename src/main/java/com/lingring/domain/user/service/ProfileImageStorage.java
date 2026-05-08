package com.lingring.domain.user.service;

public interface ProfileImageStorage {

    PresignedUploadUrl generateUploadUrl(String key, String contentType, long contentLength);

    boolean exists(String key);

    String publicUrl(String key);

    void delete(String key);
}
