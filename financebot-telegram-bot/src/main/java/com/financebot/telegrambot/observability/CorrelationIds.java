package com.financebot.telegrambot.observability;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIds {
    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIds() {
    }

    public static String currentOrCreate() {
        String current = MDC.get(MDC_KEY);
        return current == null || current.isBlank() ? UUID.randomUUID().toString() : current;
    }
}
