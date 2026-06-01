package com.financebot.transaction.adapter.in.web.mapper;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.SortDirection;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SpringPageMapper {

    private static final String DEFAULT_SORT_PROPERTY = "date";

    private SpringPageMapper() {
    }

    public static PageQuery toPageQuery(Pageable pageable) {
        Sort.Order order = pageable.getSort()
                .stream()
                .findFirst()
                .orElse(Sort.Order.desc(DEFAULT_SORT_PROPERTY));

        return new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                order.getProperty(),
                order.isAscending() ? SortDirection.ASC : SortDirection.DESC
        );
    }

    public static <T> PageImpl<T> toSpringPage(PageResult<T> pageResult) {
        Pageable pageable = PageRequest.of(
                pageResult.page(),
                pageResult.size()
        );

        return new PageImpl<>(
                pageResult.content(),
                pageable,
                pageResult.totalElements()
        );
    }
}