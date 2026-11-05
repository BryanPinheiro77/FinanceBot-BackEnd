package com.financebot.auth.dto.response;

import java.time.Instant;

public record SessionActivity(Instant lastActivityAt, Instant idleWarningAt, Instant idleExpiresAt,
                              Instant sessionExpiresAt) { }
