package com.financebot.transaction.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class IdempotencyFingerprint {
    private final ObjectMapper json;

    public String keyHash(String key) {
        if (!key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}")) {
            throw new IllegalArgumentException("Idempotency-Key deve ter de 8 a 128 caracteres alfanuméricos ou . _ : -.");
        }
        return digest(key.getBytes(StandardCharsets.UTF_8));
    }

    public String requestHash(Object request) {
        return digest(json.writeValueAsBytes(canonical(json.valueToTree(request))));
    }

    private Object canonical(JsonNode node) {
        if (node.isObject()) {
            var values = new TreeMap<String, Object>();
            node.properties().forEach(entry -> values.put(entry.getKey(), canonical(entry.getValue())));
            return values;
        }
        if (node.isArray()) {
            var values = new ArrayList<>();
            node.forEach(value -> values.add(canonical(value)));
            return values;
        }
        if (node.isNumber()) return node.decimalValue().stripTrailingZeros();
        if (node.isBoolean()) return node.asBoolean();
        return node.isNull() ? null : node.asString();
    }

    private String digest(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
