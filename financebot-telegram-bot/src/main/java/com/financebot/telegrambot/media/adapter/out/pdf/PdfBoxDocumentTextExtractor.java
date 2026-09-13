package com.financebot.telegrambot.media.adapter.out.pdf;

import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.DocumentTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Component
public class PdfBoxDocumentTextExtractor implements DocumentTextExtractor {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String PDF_EXTENSION = ".pdf";
    private static final int DEFAULT_MAX_BYTES = 10 * 1024 * 1024;

    private final int maxBytes;

    public PdfBoxDocumentTextExtractor() {
        this(DEFAULT_MAX_BYTES);
    }

    PdfBoxDocumentTextExtractor(int maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("O limite do PDF deve ser positivo");
        }
        this.maxBytes = maxBytes;
    }

    @Override
    public boolean supports(String contentType, String fileName) {
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return PDF_CONTENT_TYPE.equals(normalizedType) || normalizedName.endsWith(PDF_EXTENSION);
    }

    @Override
    public String extract(InputStream content) {
        if (content == null) {
            throw new MediaExtractionException("O conteúdo do PDF é obrigatório");
        }

        try {
            byte[] pdfBytes = readWithinLimit(content);
            try (var document = Loader.loadPDF(pdfBytes)) {
                String extractedText = new PDFTextStripper().getText(document).trim();
                if (extractedText.isBlank()) {
                    throw new MediaExtractionException("Não foi encontrado texto no PDF");
                }
                return extractedText;
            }
        } catch (MediaExtractionException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new MediaExtractionException("Não foi possível extrair texto do PDF", exception);
        }
    }

    private byte[] readWithinLimit(InputStream content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = content.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new MediaExtractionException("O PDF excede o tamanho máximo permitido");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
