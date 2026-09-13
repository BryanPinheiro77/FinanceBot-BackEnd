package com.financebot.telegrambot.media.adapter.out.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financebot.telegrambot.config.AiProperties;
import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.AudioTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenAiAudioTextExtractor implements AudioTextExtractor {

    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024;
    private static final String CRLF = "\r\n";

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extract(InputStream content, String fileName, String contentType) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()
                || properties.transcriptionEndpoint() == null || properties.transcriptionEndpoint().isBlank()) {
            throw new MediaExtractionException("Transcrição de áudio não está configurada");
        }
        if (content == null) {
            throw new MediaExtractionException("Áudio inválido para transcrição");
        }

        String boundary = "----FinanceBot" + UUID.randomUUID();
        try {
            byte[] audio = readWithinLimit(content);
            byte[] body = multipartBody(boundary, fileName, contentType, audio);
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.transcriptionEndpoint()))
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MediaExtractionException("O provedor de transcrição recusou o áudio");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new MediaExtractionException("Não foi encontrada fala no áudio");
            }
            return text.trim();
        } catch (MediaExtractionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MediaExtractionException("Não foi possível transcrever o áudio", exception);
        }
    }

    private byte[] multipartBody(String boundary, String fileName, String contentType, byte[] audio) {
        String safeName = fileName == null || fileName.isBlank() ? "telegram-audio.ogg" : fileName;
        String safeType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "--" + boundary + CRLF);
        write(output, "Content-Disposition: form-data; name=\"model\"" + CRLF + CRLF);
        write(output, properties.transcriptionModel() + CRLF);
        write(output, "--" + boundary + CRLF);
        write(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"" + CRLF);
        write(output, "Content-Type: " + safeType + CRLF + CRLF);
        output.writeBytes(audio);
        write(output, CRLF + "--" + boundary + "--" + CRLF);
        return output.toByteArray();
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] readWithinLimit(InputStream content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = content.read(buffer)) != -1) {
            total += read;
            if (total > MAX_AUDIO_BYTES) {
                throw new MediaExtractionException("O áudio excede o tamanho máximo permitido");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
