package com.financebot.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.financebot.security.crypto.EncryptedBigDecimalConverter;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_telegram_id", columnNames = "telegram_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "monthly_base_income_encrypted")
    @Convert(converter = EncryptedBigDecimalConverter.class)
    private BigDecimal monthlyBaseIncome;

    /**
     * Coluna legada usada somente durante a migração gradual para o valor cifrado.
     */
    @Column(name = "monthly_base_income", precision = 15, scale = 2)
    private BigDecimal monthlyBaseIncomeLegacy;

    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "telegram_link_code", length = 30)
    private String telegramLinkCode;

    @Column(name = "telegram_link_code_expires_at")
    private LocalDateTime telegramLinkCodeExpiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.role == null) {
            this.role = UserRole.USER;
        }

        if (this.onboardingCompleted == null) {
            this.onboardingCompleted = false;
        }

        this.monthlyBaseIncomeLegacy = null;
    }

    @PostLoad
    public void migrateLegacyMonthlyBaseIncome() {
        if (this.monthlyBaseIncome == null) {
            this.monthlyBaseIncome = this.monthlyBaseIncomeLegacy;
        }
    }

    @PreUpdate
    public void clearLegacyMonthlyBaseIncome() {
        this.monthlyBaseIncomeLegacy = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
