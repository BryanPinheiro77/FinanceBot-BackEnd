package com.financebot.security.rotation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class DataEncryptionRotationRepository {

    private final JdbcTemplate jdbcTemplate;

    DataEncryptionRotationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<EncryptedFieldRotationCandidate> findNextBatch(String activePrefix, int batchSize) {
        return jdbcTemplate.query(
                """
                SELECT id, monthly_base_income_encrypted, monthly_base_income
                FROM users
                WHERE monthly_base_income IS NOT NULL
                   OR (monthly_base_income_encrypted IS NOT NULL
                       AND SUBSTRING(monthly_base_income_encrypted, 1, ?) <> ?)
                ORDER BY id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """,
                (resultSet, rowNumber) -> new EncryptedFieldRotationCandidate(
                        resultSet.getLong("id"),
                        resultSet.getString("monthly_base_income_encrypted"),
                        resultSet.getBigDecimal("monthly_base_income")
                ),
                activePrefix.length(),
                activePrefix,
                batchSize
        );
    }

    int replaceEncryptedValue(Long userId, String previousValue, String rotatedValue) {
        return jdbcTemplate.update(
                """
                UPDATE users
                SET monthly_base_income_encrypted = ?, monthly_base_income = NULL
                WHERE id = ? AND monthly_base_income_encrypted = ?
                """,
                rotatedValue,
                userId,
                previousValue
        );
    }

    int migrateLegacyValue(Long userId, String rotatedValue) {
        return jdbcTemplate.update(
                """
                UPDATE users
                SET monthly_base_income_encrypted = ?, monthly_base_income = NULL
                WHERE id = ?
                  AND monthly_base_income_encrypted IS NULL
                  AND monthly_base_income IS NOT NULL
                """,
                rotatedValue,
                userId
        );
    }
}
