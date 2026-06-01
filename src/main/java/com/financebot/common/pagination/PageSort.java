package com.financebot.common.pagination;

public record PageSort(
        String property,
        SortDirection direction
) {
}