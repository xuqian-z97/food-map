package com.foodmap.common.api;

import java.util.List;

/**
 * 统一分页响应结构，用于列表、地图点位和评论列表等分页查询。
 *
 * <p>{@code pageIndex} 采用从 0 开始的后端分页索引，前端展示页码时可以自行转换为从 1 开始。</p>
 */
public record PageResponse<T>(
        List<T> items,
        long total,
        int pageIndex,
        int pageSize,
        boolean hasMore
) {
    /**
     * 根据总数和当前页参数创建分页响应，并统一计算是否还有下一页。
     *
     * @param items 当前页数据列表。
     * @param total 符合查询条件的数据总数。
     * @param pageIndex 当前页索引，后端统一从 0 开始。
     * @param pageSize 当前页大小。
     * @return 包含分页数据和是否存在下一页标记的统一响应。
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int pageIndex, int pageSize) {
        long nextOffset = (long) (pageIndex + 1) * pageSize;
        return new PageResponse<>(items, total, pageIndex, pageSize, nextOffset < total);
    }
}
