package com.financebot.telegrambot.media.application;

import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.DocumentTextExtractor;
import com.financebot.telegrambot.media.application.port.out.ImageTextExtractor;
import com.financebot.telegrambot.media.application.port.out.TelegramFileDownloader;
import com.financebot.telegrambot.service.TelegramCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class TelegramMediaMessageHandler {

    private final DocumentTextExtractor documentTextExtractor;
    private final ImageTextExtractor imageTextExtractor;
    private final TelegramFileDownloader telegramFileDownloader;
    private final TelegramCommandService telegramCommandService;

    public String handle(
            Message message,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        if (message == null) {
            return unsupportedMediaMessage();
        }

        Document document = message.getDocument();
        if (document == null && message.hasPhoto()) {
            return handlePhoto(message, telegramId, telegramUsername, telegramFirstName);
        }
        if (document == null) {
            return "Por enquanto consigo processar documentos PDF e imagens. Áudios serão adicionados em breve.";
        }

        if (!documentTextExtractor.supports(document.getMimeType(), document.getFileName())) {
            return "Por enquanto consigo processar apenas documentos PDF. Imagens e áudios serão adicionados em breve.";
        }

        try (InputStream content = telegramFileDownloader.download(document.getFileId())) {
            String extractedText = documentTextExtractor.extract(content);
            return telegramCommandService.handleMessage(
                    extractedText,
                    telegramId,
                    telegramUsername,
                    telegramFirstName
            );
        } catch (MediaExtractionException exception) {
            return "Não consegui ler esse PDF. Envie um arquivo PDF válido com texto e tente novamente.";
        } catch (Exception exception) {
            return "Não consegui processar esse arquivo agora. Tente novamente em alguns instantes.";
        }
    }

    private String unsupportedMediaMessage() {
        return "Por enquanto consigo processar documentos PDF e imagens. Áudios serão adicionados em breve.";
    }

    private String handlePhoto(
            Message message,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        PhotoSize photo = message.getPhoto().stream()
                .max(java.util.Comparator.comparing(PhotoSize::getFileSize,
                        java.util.Comparator.nullsFirst(Integer::compareTo)))
                .orElse(null);
        if (photo == null) {
            return "Não consegui identificar a imagem enviada.";
        }

        try (InputStream content = telegramFileDownloader.download(photo.getFileId())) {
            String extractedText = imageTextExtractor.extract(content, "image/jpeg");
            return telegramCommandService.handleMessage(
                    extractedText,
                    telegramId,
                    telegramUsername,
                    telegramFirstName
            );
        } catch (MediaExtractionException exception) {
            return "Não consegui ler o texto dessa imagem. Verifique se ela está nítida e tente novamente.";
        } catch (Exception exception) {
            return "Não consegui processar essa imagem agora. Tente novamente em alguns instantes.";
        }
    }
}
