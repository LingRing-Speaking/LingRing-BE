package com.lingring.domain.call.domain;

import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.List;

public interface TranscriptionStarter {

    void requestTranscript(Long callId, List<RecordingReference> recordings);
}
