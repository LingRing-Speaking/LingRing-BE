package com.lingring.domain.call.domain.port;

import java.util.List;
import java.util.Set;

public interface RecordingReadinessProvider {

    Set<Long> findReadyCallIds(List<Long> callIds);
}
