package com.financebot.telegrambot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "financebot.ai")
public record AiProperties(
        boolean enabled,
        String endpoint,
        String apiKey,
        String model,
        Duration timeout,
        String transcriptionEndpoint,
        String transcriptionModel
) {

    public AiProperties {
        model = model == null || model.isBlank() ? "gpt-4o-mini" : model;
        timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
        transcriptionModel = transcriptionModel == null || transcriptionModel.isBlank()
                ? "whisper-1" : transcriptionModel;
    }
}
