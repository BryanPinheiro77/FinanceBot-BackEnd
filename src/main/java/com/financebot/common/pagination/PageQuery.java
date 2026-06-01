package com.financebot.common.pagination;

import java.util.List;

public record PageQuery(
        int page,
        int size,
        List<PageSort> sorts
) {
}