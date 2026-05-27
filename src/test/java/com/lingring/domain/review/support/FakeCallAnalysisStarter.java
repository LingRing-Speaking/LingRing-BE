package com.lingring.domain.review.support;

import com.lingring.domain.review.domain.recording.vo.RecordingReference;
import com.lingring.domain.review.domain.port.CallAnalysisStarter;
import java.util.ArrayList;
import java.util.List;

public class FakeCallAnalysisStarter implements CallAnalysisStarter {

    private final List<Invocation> invocations = new ArrayList<>();

    @Override
    public void requestAnalysis(final Long callId, final List<RecordingReference> recordings) {
        invocations.add(new Invocation(callId, List.copyOf(recordings)));
    }

    public List<Invocation> invocations() {
        return List.copyOf(invocations);
    }

    public void reset() {
        invocations.clear();
    }

    public record Invocation(Long callId, List<RecordingReference> recordings) {
    }
}
