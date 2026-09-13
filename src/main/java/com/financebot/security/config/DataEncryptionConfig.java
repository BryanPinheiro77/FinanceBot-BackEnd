package com.financebot.security.config;

import com.financebot.security.crypto.EncryptionKeyHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class DataEncryptionConfig {

    private final String base64Key;
    private final String activeKeyId;
    private final String legacyKeyId;
    private final String previousKeys;

    public DataEncryptionConfig(
            @Value("${app.data-encryption.key:}") String base64Key,
            @Value("${app.data-encryption.active-key-id:primary}") String activeKeyId,
            @Value("${app.data-encryption.legacy-key-id:${app.data-encryption.active-key-id:primary}}") String legacyKeyId,
            @Value("${app.data-encryption.previous-keys:}") String previousKeys
    ) {
        this.base64Key = base64Key;
        this.activeKeyId = activeKeyId;
        this.legacyKeyId = legacyKeyId;
        this.previousKeys = previousKeys;
    }

    @PostConstruct
    void configureEncryptionKey() {
        EncryptionKeyHolder.configure(activeKeyId, base64Key, legacyKeyId, previousKeys);
    }
}
