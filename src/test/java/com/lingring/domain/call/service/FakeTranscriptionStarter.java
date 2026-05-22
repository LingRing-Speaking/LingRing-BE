package com.lingring.domain.call.service;

import com.lingring.domain.call.domain.TranscriptionStarter;
import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.ArrayList;
import java.util.List;

public class FakeTranscriptionStarter implements TranscriptionStarter {

    private final List<Invocation> invocations = new ArrayList<>();

    @Override
    public void requestTranscript(final Long callId, final List<RecordingReference> recordings) {
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
