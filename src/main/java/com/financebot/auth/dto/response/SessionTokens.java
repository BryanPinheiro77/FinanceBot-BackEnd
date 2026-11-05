package com.financebot.auth.dto.response;

import java.time.Instant;

public record SessionTokens(String token, String refreshToken, Instant sessionExpiresAt,
                            Instant lastActivityAt) { }
