package com.lingring.infrastructure.s3;

import com.lingring.domain.user.domain.PresignedUploadUrl;
import com.lingring.domain.user.domain.ProfileImageStorage;
import java.util.HashSet;
import java.util.Set;

public class FakeProfileImageStorage implements ProfileImageStorage {

    private static final String FAKE_HOST = "https://fake-s3.local";
    private static final String FAKE_CDN = "https://fake-cdn.local";

    private final Set<String> uploadedKeys = new HashSet<>();
    private final Set<String> deletedKeys = new HashSet<>();

    @Override
    public PresignedUploadUrl generateUploadUrl(
            final String key,
            final String contentType,
            final long contentLength
    ) {
        return new PresignedUploadUrl(FAKE_HOST + "/" + key, key);
    }

    @Override
    public boolean exists(final String key) {
        return uploadedKeys.contains(key);
    }

    @Override
    public String publicUrl(final String key) {
        return FAKE_CDN + "/" + key;
    }

    @Override
    public void delete(final String key) {
        uploadedKeys.remove(key);
        deletedKeys.add(key);
    }

    public void simulateUpload(final String key) {
        uploadedKeys.add(key);
    }

    public Set<String> deletedKeys() {
        return Set.copyOf(deletedKeys);
    }

    public void clear() {
        uploadedKeys.clear();
        deletedKeys.clear();
    }
}
