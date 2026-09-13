package com.financebot.security.rotation;

import com.financebot.security.config.DataEncryptionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({DataEncryptionConfig.class, DataEncryptionRotationRepository.class})
class DataEncryptionRotationRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataEncryptionRotationRepository repository;

    @Test
    void shouldSelectOnlyLegacyOrNonActiveValuesAndUpdateThemOptimistically() {
        long legacyId = insertUser("legacy@example.com", null, new BigDecimal("3000.00"));
        long previousId = insertUser("previous@example.com", "v1.previous-payload", null);
        long mixedId = insertUser(
                "mixed@example.com",
                "v2.current.current-payload",
                new BigDecimal("5000.00")
        );
        insertUser("current@example.com", "v2.current.current-payload", null);

        var candidates = repository.findNextBatch("v2.current.", 10);

        assertThat(candidates).extracting(EncryptedFieldRotationCandidate::userId)
                .containsExactly(legacyId, previousId, mixedId);
        assertThat(repository.migrateLegacyValue(legacyId, "v2.current.rotated-legacy")).isEqualTo(1);
        assertThat(repository.replaceEncryptedValue(
                previousId,
                "v1.previous-payload",
                "v2.current.rotated-previous"
        )).isEqualTo(1);
        assertThat(repository.replaceEncryptedValue(
                previousId,
                "v1.previous-payload",
                "v2.current.conflict"
        )).isZero();
        assertThat(repository.replaceEncryptedValue(
                mixedId,
                "v2.current.current-payload",
                "v2.current.rotated-mixed"
        )).isEqualTo(1);

        assertThat(repository.findNextBatch("v2.current.", 10)).isEmpty();
    }

    private long insertUser(String email, String encryptedIncome, BigDecimal legacyIncome) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    name, email, password, role, onboarding_completed, created_at,
                    monthly_base_income_encrypted, monthly_base_income
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
                """,
                "Usuário de teste",
                email,
                "hash-de-teste",
                "USER",
                false,
                encryptedIncome,
                legacyIncome
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }
}
