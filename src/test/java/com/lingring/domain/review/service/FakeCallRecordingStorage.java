package com.lingring.domain.review.service;

import com.lingring.domain.review.domain.port.CallRecordingStorage;
import com.lingring.domain.review.domain.port.PresignedUpload;
import java.util.HashMap;
import java.util.Map;

public class FakeCallRecordingStorage implements CallRecordingStorage {

    private final Map<String, String> storedContentTypes = new HashMap<>();

    @Override
    public PresignedUpload generateUploadUrl(
            final String key,
            final String contentType,
            final long contentLength
    ) {
        return new PresignedUpload("https://fake-s3.test/upload/%s".formatted(key), key);
    }

    @Override
    public boolean exists(final String key) {
        return storedContentTypes.containsKey(key);
    }

    @Override
    public String contentTypeOf(final String key) {
        return storedContentTypes.get(key);
    }

    public void put(final String key, final String contentType) {
        storedContentTypes.put(key, contentType);
    }

    public void reset() {
        storedContentTypes.clear();
    }
}
