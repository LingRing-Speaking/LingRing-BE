package com.lingring.domain.callanalysis.domain.port;

import com.lingring.domain.callanalysis.domain.recording.vo.RecordingReference;
import java.util.List;

public interface CallAnalysisStarter {

    void requestAnalysis(Long callId, List<RecordingReference> recordings);
}
