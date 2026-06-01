package com.financebot.common.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(
                content.stream()
                        .map(mapper)
                        .toList(),
                page,
                size,
                totalElements,
                totalPages,
                first,
                last
        );
    }
}