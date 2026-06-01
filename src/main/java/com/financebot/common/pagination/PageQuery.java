package com.financebot.common.pagination;

public record PageQuery(
        int page,
        int size,
        String sortBy,
        SortDirection direction
) {
}