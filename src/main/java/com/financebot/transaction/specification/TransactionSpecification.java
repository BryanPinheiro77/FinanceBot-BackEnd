package com.financebot.transaction.specification;

import com.financebot.transaction.domain.Transaction;
import com.financebot.transaction.dto.TransactionFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionSpecification {

    private static final String USER_FIELD = "user";
    private static final String ID_FIELD = "id";
    private static final String TYPE_FIELD = "type";
    private static final String CATEGORY_FIELD = "category";
    private static final String ACCOUNT_FIELD = "account";
    private static final String DATE_FIELD = "date";
    private static final String SOURCE_TYPE_FIELD = "sourceType";
    private static final String DESCRIPTION_FIELD = "description";

    private TransactionSpecification() {
    }

    public static Specification<Transaction> withFilters(Long userId, TransactionFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            addUserPredicate(predicates, root, criteriaBuilder, userId);

            if (filter != null) {
                addTypePredicate(predicates, root, criteriaBuilder, filter);
                addCategoryPredicate(predicates, root, criteriaBuilder, filter);
                addAccountPredicate(predicates, root, criteriaBuilder, filter);
                addDatePredicate(predicates, root, criteriaBuilder, filter);
                addSourceTypePredicate(predicates, root, criteriaBuilder, filter);
                addDescriptionPredicate(predicates, root, criteriaBuilder, filter);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addUserPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            Long userId
    ) {
        predicates.add(criteriaBuilder.equal(root.get(USER_FIELD).get(ID_FIELD), userId));
    }

    private static void addTypePredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.type() != null) {
            predicates.add(criteriaBuilder.equal(root.get(TYPE_FIELD), filter.type()));
        }
    }

    private static void addCategoryPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.categoryId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(CATEGORY_FIELD).get(ID_FIELD), filter.categoryId()));
        }
    }

    private static void addAccountPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.accountId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(ACCOUNT_FIELD).get(ID_FIELD), filter.accountId()));
        }
    }

    private static void addDatePredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.startDate() != null && filter.endDate() != null) {
            predicates.add(criteriaBuilder.between(root.get(DATE_FIELD), filter.startDate(), filter.endDate()));
            return;
        }

        if (filter.startDate() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(DATE_FIELD), filter.startDate()));
            return;
        }

        if (filter.endDate() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(DATE_FIELD), filter.endDate()));
        }
    }

    private static void addSourceTypePredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.sourceType() != null) {
            predicates.add(criteriaBuilder.equal(root.get(SOURCE_TYPE_FIELD), filter.sourceType()));
        }
    }

    private static void addDescriptionPredicate(
            List<Predicate> predicates,
            Root<Transaction> root,
            CriteriaBuilder criteriaBuilder,
            TransactionFilter filter
    ) {
        if (filter.description() == null || filter.description().isBlank()) {
            return;
        }

        String normalizedDescription = "%" + filter.description().toLowerCase(Locale.ROOT).trim() + "%";

        predicates.add(
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(DESCRIPTION_FIELD)),
                        normalizedDescription
                )
        );
    }
}