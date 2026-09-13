package com.financebot.security.config;

import com.financebot.security.crypto.EncryptionKeyHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class DataEncryptionConfig {

    private final String base64Key;

    public DataEncryptionConfig(@Value("${app.data-encryption.key:}") String base64Key) {
        this.base64Key = base64Key;
    }

    @PostConstruct
    void configureEncryptionKey() {
        EncryptionKeyHolder.configure(base64Key);
    }
}
