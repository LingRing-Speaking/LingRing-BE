package com.lingring.domain.user.domain.port;

import java.util.List;

public record ModerationVerdict(
        boolean inappropriate,
        List<String> reasons
) {

    public static ModerationVerdict acceptable() {
        return new ModerationVerdict(false, List.of());
    }

    public static ModerationVerdict reject(final List<String> reasons) {
        return new ModerationVerdict(true, List.copyOf(reasons));
    }
}
