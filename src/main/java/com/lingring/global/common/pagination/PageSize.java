package com.lingring.global.common.pagination;

public record PageSize(int value) {

    private static final int MIN = 1;
    private static final int MAX = 50;

    public static PageSize clamp(final int requested) {
        return new PageSize(Math.min(Math.max(requested, MIN), MAX));
    }
}