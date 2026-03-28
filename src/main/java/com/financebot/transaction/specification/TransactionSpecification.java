package com.financebot.transaction.specification;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> withFilters(Long userId, TransactionFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (filter != null) {
                if (filter.type() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("type"), filter.type()));
                }

                if (filter.categoryId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.categoryId()));
                }

                if (filter.accountId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("account").get("id"), filter.accountId()));
                }

                if (filter.startDate() != null && filter.endDate() != null) {
                    predicates.add(criteriaBuilder.between(root.get("date"), filter.startDate(), filter.endDate()));
                } else if (filter.startDate() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), filter.startDate()));
                } else if (filter.endDate() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), filter.endDate()));
                }

                if (filter.sourceType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("sourceType"), filter.sourceType()));
                }

                if (filter.description() != null && !filter.description().isBlank()) {
                    predicates.add(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("description")),
                                    "%" + filter.description().toLowerCase().trim() + "%"
                            )
                    );
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}