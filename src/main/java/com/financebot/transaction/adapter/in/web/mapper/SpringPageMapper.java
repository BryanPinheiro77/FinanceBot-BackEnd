package com.financebot.transaction.adapter.in.web.mapper;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.PageSort;
import com.financebot.common.pagination.SortDirection;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class SpringPageMapper {

    private SpringPageMapper() {
    }

    public static PageQuery toPageQuery(Pageable pageable) {
        List<PageSort> sorts = pageable.getSort()
                .stream()
                .map(order -> new PageSort(
                        order.getProperty(),
                        order.isAscending() ? SortDirection.ASC : SortDirection.DESC
                ))
                .toList();

        return new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sorts
        );
    }

    public static <T> PageImpl<T> toSpringPage(PageResult<T> pageResult, Pageable pageable) {
        return new PageImpl<>(
                pageResult.content(),
                pageable,
                pageResult.totalElements()
        );
    }
}