package com.foodmap.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int pageIndex,
        int pageSize,
        boolean hasMore
) {
    public static <T> PageResponse<T> of(List<T> items, long total, int pageIndex, int pageSize) {
        long nextOffset = (long) (pageIndex + 1) * pageSize;
        return new PageResponse<>(items, total, pageIndex, pageSize, nextOffset < total);
    }
}
