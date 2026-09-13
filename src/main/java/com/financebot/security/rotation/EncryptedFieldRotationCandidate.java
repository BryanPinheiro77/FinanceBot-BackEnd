package com.financebot.security.rotation;

import java.math.BigDecimal;

record EncryptedFieldRotationCandidate(Long userId, String encryptedValue, BigDecimal legacyValue) {
}
