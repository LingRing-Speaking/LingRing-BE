package com.lingring.domain.review.domain.analysis.vo;

import java.util.List;
import java.util.Objects;

public record Mistakes(List<MistakeItem> items) {

    public Mistakes {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }

    public static Mistakes empty() {
        return new Mistakes(List.of());
    }

    public int count() {
        return items.size();
    }
}
