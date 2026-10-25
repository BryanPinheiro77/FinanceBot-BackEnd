package com.financebot.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.auth-rate-limit")
public class AuthRateLimitProperties {
    private boolean enabled = true;
    private String webProxySecret = "";
    @Valid @NotNull private Rule login = new Rule(10, 60);
    @Valid @NotNull private Rule register = new Rule(5, 3600);
    @NotNull private List<String> trustedProxies = List.of();

    @Getter
    @Setter
    public static class Rule {
        @Min(1) @Max(100000) private int maxRequests;
        @Min(1) @Max(86400) private long windowSeconds;

        public Rule() { }

        public Rule(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
