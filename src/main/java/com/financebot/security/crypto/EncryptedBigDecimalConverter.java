package com.financebot.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal value) {
        return value == null ? null : EncryptionKeyHolder.get().encrypt(value.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String value) {
        return value == null ? null : new BigDecimal(EncryptionKeyHolder.get().decrypt(value));
    }
}
