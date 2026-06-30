package com.lingring.domain.review.domain.policy;

import com.lingring.domain.review.exception.CallTooShortException;
import org.springframework.stereotype.Component;

@Component
public class CallAnalysisPolicy {

    private static final long MIN_DURATION_SEC = 60L;

    public void requireEnoughToAnalyze(final Long callId, final Long durationSec) {
        if (durationSec == null || durationSec < MIN_DURATION_SEC) {
            throw new CallTooShortException(callId);
        }
    }
}
