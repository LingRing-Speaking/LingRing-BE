package com.lingring.domain.call.domain.port;

import java.util.List;
import java.util.Map;

public interface AnalysisSummaryProvider {

    Map<Long, AnalysisSummaryView> findByCallIds(Long userId, List<Long> callIds);
}
