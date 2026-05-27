package com.lingring.domain.review.domain.port;

import com.lingring.domain.review.domain.recording.vo.RecordingReference;
import java.util.List;

public interface CallAnalysisStarter {

    void requestAnalysis(Long callId, List<RecordingReference> recordings);
}
