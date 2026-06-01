package com.financebot.transaction.adapter.in.web.mapper;

import com.financebot.common.pagination.PageQuery;
import com.financebot.common.pagination.PageResult;
import com.financebot.common.pagination.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringPageMapperTest {

    @Test
    @DisplayName("deve converter pageable sem sort para page query sem sort")
    void shouldConvertUnsortedPageableToUnsortedPageQuery() {
        PageRequest pageable = PageRequest.of(0, 10);

        PageQuery result = SpringPageMapper.toPageQuery(pageable);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.sorts()).isEmpty();
    }

    @Test
    @DisplayName("deve converter pageable com sort asc para page query")
    void shouldConvertAscendingSortToPageQuery() {
        PageRequest pageable = PageRequest.of(
                1,
                20,
                Sort.by(Sort.Direction.ASC, "amount")
        );

        PageQuery result = SpringPageMapper.toPageQuery(pageable);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.sorts()).hasSize(1);
        assertThat(result.sorts().get(0).property()).isEqualTo("amount");
        assertThat(result.sorts().get(0).direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    @DisplayName("deve converter pageable com sort desc para page query")
    void shouldConvertDescendingSortToPageQuery() {
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "date")
        );

        PageQuery result = SpringPageMapper.toPageQuery(pageable);

        assertThat(result.sorts()).hasSize(1);
        assertThat(result.sorts().get(0).property()).isEqualTo("date");
        assertThat(result.sorts().get(0).direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    @DisplayName("deve preservar multiplos sorts ao converter pageable para page query")
    void shouldPreserveMultipleSortsWhenConvertingToPageQuery() {
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Order.desc("date"),
                        Sort.Order.asc("amount")
                )
        );

        PageQuery result = SpringPageMapper.toPageQuery(pageable);

        assertThat(result.sorts()).hasSize(2);

        assertThat(result.sorts().get(0).property()).isEqualTo("date");
        assertThat(result.sorts().get(0).direction()).isEqualTo(SortDirection.DESC);

        assertThat(result.sorts().get(1).property()).isEqualTo("amount");
        assertThat(result.sorts().get(1).direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    @DisplayName("deve converter page result para page preservando pageable original")
    void shouldConvertPageResultToSpringPagePreservingOriginalPageable() {
        PageRequest pageable = PageRequest.of(
                2,
                5,
                Sort.by(Sort.Direction.DESC, "date")
        );

        PageResult<String> pageResult = new PageResult<>(
                List.of("item-1", "item-2"),
                2,
                5,
                12,
                3,
                false,
                true
        );

        Page<String> result = SpringPageMapper.toSpringPage(pageResult, pageable);

        assertThat(result.getContent()).containsExactly("item-1", "item-2");
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getSort()).isEqualTo(pageable.getSort());
    }
}