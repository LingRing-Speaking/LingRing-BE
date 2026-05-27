package com.lingring.domain.callanalysis.domain.analysis.vo;

import java.util.List;
import java.util.Objects;

public record Positives(List<PositiveItem> items) {

    public Positives {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }

    public static Positives empty() {
        return new Positives(List.of());
    }

    public int count() {
        return items.size();
    }
}
