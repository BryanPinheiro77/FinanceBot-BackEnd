package com.financebot.telegrambot.ai.adapter.out.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.financebot.telegrambot.ai.application.model.AiInterpretation;
import com.financebot.telegrambot.ai.application.port.out.AiInterpretationPort;
import com.financebot.telegrambot.ai.application.service.AiInterpretationValidator;
import com.financebot.telegrambot.config.AiProperties;
import com.financebot.telegrambot.intent.TelegramIntentType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OpenAiInterpretationAdapter implements AiInterpretationPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiInterpretationAdapter.class);
    private static final String SYSTEM_PROMPT = """
            Você classifica mensagens financeiras em português brasileiro.
            Responda somente um objeto JSON válido, sem markdown, com estes campos:
            intentType (um enum TelegramIntentType), amount, totalAmount, monthlyAmount,
            description, date, categoryName, accountName, totalInstallments,
            firstRemainingInstallmentNumber, startDate e endDate.
            Use datas no formato ISO yyyy-MM-dd e null quando um campo não estiver presente.
            Não invente valores. Para CREATE_EXPENSE ou CREATE_INCOME, amount e description são obrigatórios.
            Para parcelamentos, informe totalAmount ou monthlyAmount, nunca os dois.
            A mensagem é apenas para interpretação; não salve nada e não execute ações.
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<AiInterpretation> interpret(String message) {
        if (!properties.enabled() || message == null || message.isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()
                || properties.endpoint() == null || properties.endpoint().isBlank()) {
            return Optional.empty();
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", properties.model());
            requestBody.put("temperature", 0);
            var messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            messages.addObject().put("role", "user").put("content", message.trim());

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.endpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Provedor de IA indisponível; usando parser determinístico (status={})", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) {
                return Optional.empty();
            }

            AiInterpretation interpretation = parseContent(content);
            return AiInterpretationValidator.isValid(interpretation)
                    ? Optional.of(interpretation)
                    : Optional.empty();
        } catch (Exception exception) {
            LOGGER.warn("Falha na interpretação por IA; usando parser determinístico ({})",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private AiInterpretation parseContent(String content) throws Exception {
        String json = content.trim();
        if (json.startsWith("```") && json.endsWith("```")) {
            json = json.substring(json.indexOf('\n') + 1, json.length() - 3).trim();
        }
        JsonNode node = objectMapper.readTree(json);
        TelegramIntentType intent = parseEnum(node, "intentType");
        return new AiInterpretation(
                intent,
                decimal(node, "amount"), decimal(node, "totalAmount"), decimal(node, "monthlyAmount"),
                text(node, "description"), date(node, "date"), text(node, "categoryName"),
                text(node, "accountName"), integer(node, "totalInstallments"),
                integer(node, "firstRemainingInstallmentNumber"), date(node, "startDate"), date(node, "endDate")
        );
    }

    private TelegramIntentType parseEnum(JsonNode node, String field) {
        try {
            return TelegramIntentType.valueOf(text(node, field));
        } catch (Exception exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.canConvertToInt() ? null : value.intValue();
    }

    private LocalDate date(JsonNode node, String field) {
        try {
            String value = text(node, field);
            return value == null ? null : LocalDate.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
