package com.financebot.telegrambot.media.application;

import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.DocumentTextExtractor;
import com.financebot.telegrambot.media.application.port.out.TelegramFileDownloader;
import com.financebot.telegrambot.service.TelegramCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramMediaMessageHandlerTest {

    @Mock private DocumentTextExtractor extractor;
    @Mock private TelegramFileDownloader downloader;
    @Mock private TelegramCommandService commandService;

    private TelegramMediaMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TelegramMediaMessageHandler(extractor, downloader, commandService);
    }

    @Test
    void shouldSendExtractedPdfTextToExistingCommandFlow() {
        Document document = pdfDocument();
        Message message = Message.builder().document(document).build();
        when(extractor.supports("application/pdf", "fatura.pdf")).thenReturn(true);
        when(downloader.download("file-id")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(extractor.extract(org.mockito.ArgumentMatchers.any())).thenReturn("gastei 50 no mercado");
        when(commandService.handleMessage("gastei 50 no mercado", 123L, "bryan", "Bryan"))
                .thenReturn("Preview");

        String response = handler.handle(message, 123L, "bryan", "Bryan");

        assertThat(response).isEqualTo("Preview");
        verify(commandService).handleMessage("gastei 50 no mercado", 123L, "bryan", "Bryan");
    }

    @Test
    void shouldRejectUnsupportedDocumentBeforeDownload() {
        Document document = new Document();
        document.setMimeType("text/plain");
        document.setFileName("nota.txt");
        Message message = Message.builder().document(document).build();
        when(extractor.supports("text/plain", "nota.txt")).thenReturn(false);

        String response = handler.handle(message, 123L, "bryan", "Bryan");

        assertThat(response).contains("apenas documentos PDF");
        verify(downloader, never()).download(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldReturnFriendlyMessageWhenExtractionFails() {
        Document document = pdfDocument();
        Message message = Message.builder().document(document).build();
        when(extractor.supports("application/pdf", "fatura.pdf")).thenReturn(true);
        when(downloader.download("file-id")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(extractor.extract(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new MediaExtractionException("invalid pdf"));

        String response = handler.handle(message, 123L, "bryan", "Bryan");

        assertThat(response).contains("Não consegui ler esse PDF");
        verify(commandService, never()).handleMessage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private Document pdfDocument() {
        Document document = new Document();
        document.setMimeType("application/pdf");
        document.setFileName("fatura.pdf");
        document.setFileId("file-id");
        return document;
    }
}
