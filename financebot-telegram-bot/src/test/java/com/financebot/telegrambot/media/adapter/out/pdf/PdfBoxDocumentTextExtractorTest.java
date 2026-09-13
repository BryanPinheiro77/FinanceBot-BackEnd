package com.financebot.telegrambot.media.adapter.out.pdf;

import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfBoxDocumentTextExtractorTest {

    @Test
    void shouldExtractTextFromPdf() throws Exception {
        PdfBoxDocumentTextExtractor extractor = new PdfBoxDocumentTextExtractor(1024 * 1024);

        assertThat(extractor.supports("application/pdf", "comprovante.bin")).isTrue();
        assertThat(extractor.extract(new ByteArrayInputStream(pdfWithText("Compra no mercado"))))
                .contains("Compra no mercado");
    }

    @Test
    void shouldSupportPdfExtension() {
        assertThat(new PdfBoxDocumentTextExtractor().supports(null, "fatura.PDF")).isTrue();
        assertThat(new PdfBoxDocumentTextExtractor().supports("text/plain", "nota.txt")).isFalse();
    }

    @Test
    void shouldRejectOversizedPdf() {
        PdfBoxDocumentTextExtractor extractor = new PdfBoxDocumentTextExtractor(2);

        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(new byte[]{1, 2, 3})))
                .isInstanceOf(MediaExtractionException.class)
                .hasMessage("O PDF excede o tamanho máximo permitido");
    }

    @Test
    void shouldRejectPdfWithoutText() {
        PdfBoxDocumentTextExtractor extractor = new PdfBoxDocumentTextExtractor();

        assertThatThrownBy(() -> extractor.extract(new ByteArrayInputStream(emptyPdf())))
                .isInstanceOf(MediaExtractionException.class)
                .hasMessage("Não foi encontrado texto no PDF");
    }

    private byte[] pdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] emptyPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
