package com.lingring.infrastructure.rekognition;

import com.lingring.domain.user.domain.ModerationVerdict;
import com.lingring.domain.user.domain.ProfileImageModerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeProfileImageModerator implements ProfileImageModerator {

    private final Map<String, List<String>> reasonsByKey = new HashMap<>();
    private int invocationCount = 0;

    @Override
    public ModerationVerdict moderate(final String key) {
        invocationCount++;
        final List<String> reasons = reasonsByKey.get(key);
        if (reasons == null) {
            return ModerationVerdict.acceptable();
        }
        return ModerationVerdict.reject(reasons);
    }

    public void markInappropriate(final String key, final List<String> reasons) {
        reasonsByKey.put(key, List.copyOf(reasons));
    }

    public int invocationCount() {
        return invocationCount;
    }

    public void clear() {
        reasonsByKey.clear();
        invocationCount = 0;
    }
}
