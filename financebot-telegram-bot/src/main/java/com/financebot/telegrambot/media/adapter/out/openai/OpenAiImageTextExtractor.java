package com.financebot.telegrambot.media.adapter.out.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.financebot.telegrambot.config.AiProperties;
import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.ImageTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class OpenAiImageTextExtractor implements ImageTextExtractor {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final String EXTRACTION_PROMPT = """
            Extraia somente o texto legível desta imagem, preservando números, datas e nomes.
            Não invente conteúdo. Se não houver texto legível, responda exatamente: SEM_TEXTO.
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extract(InputStream content, String contentType) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()
                || properties.endpoint() == null || properties.endpoint().isBlank()) {
            throw new MediaExtractionException("OCR por IA não está configurado");
        }
        if (content == null || contentType == null || !contentType.startsWith("image/")) {
            throw new MediaExtractionException("Imagem inválida para OCR");
        }

        try {
            String encodedImage = Base64.getEncoder().encodeToString(readWithinLimit(content));
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", properties.model());
            requestBody.put("temperature", 0);
            ArrayNode messages = requestBody.putArray("messages");
            messages.addObject().put("role", "system").put("content", "Você é um OCR preciso.");
            ObjectNode userMessage = messages.addObject().put("role", "user");
            ArrayNode userContent = userMessage.putArray("content");
            userContent.addObject().put("type", "text").put("text", EXTRACTION_PROMPT);
            userContent.addObject().put("type", "image_url")
                    .putObject("image_url")
                    .put("url", "data:" + contentType + ";base64," + encodedImage)
                    .put("detail", "low");

            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.endpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MediaExtractionException("O provedor de OCR recusou a imagem");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            if (text == null || text.isBlank() || "SEM_TEXTO".equalsIgnoreCase(text.trim())) {
                throw new MediaExtractionException("Não foi encontrado texto na imagem");
            }
            return text.trim();
        } catch (MediaExtractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MediaExtractionException("Não foi possível extrair texto da imagem", exception);
        }
    }

    private byte[] readWithinLimit(InputStream content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = content.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMAGE_BYTES) {
                throw new MediaExtractionException("A imagem excede o tamanho máximo permitido");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
