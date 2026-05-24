package com.lingring.domain.callanalysis.domain;

import com.lingring.domain.call.domain.vo.RecordingReference;
import java.util.List;

public interface CallAnalysisStarter {

    void requestAnalysis(Long callId, List<RecordingReference> recordings);
}
