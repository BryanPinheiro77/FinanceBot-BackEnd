package com.financebot.telegrambot.formatter;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class TelegramMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String formatTransactionPreview(
            ParsedTelegramMessage parsedMessage,
            String accountName
    ) {
        String type = parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE
                ? "despesa"
                : "receita";

        String category = parsedMessage.categoryName() != null
                ? parsedMessage.categoryName()
                : "Automática";

        return """
                💸 <b>Entendi esta %s:</b>
                
                <b>Valor:</b> %s
                <b>Descrição:</b> %s
                <b>Data:</b> %s
                <b>Conta:</b> %s
                <b>Categoria:</b> %s
                
                Deseja confirmar e salvar?
                """.formatted(
                type,
                formatCurrency(parsedMessage.amount()),
                parsedMessage.description() != null ? escapeHtml(parsedMessage.description()) : "Não informada",
                formatDate(parsedMessage.date()),
                escapeHtml(accountName),
                escapeHtml(category)
        );
    }

    public String formatUpdatedPendingMessage(
            ParsedTelegramMessage parsedMessage,
            String accountName
    ) {
        String type = parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE
                ? "despesa"
                : "receita";

        String category = parsedMessage.categoryName() != null
                ? parsedMessage.categoryName()
                : "Automática";

        return """
                ✅ <b>Operação atualizada.</b>
                
                <b>Entendi esta %s:</b>
                
                <b>Valor:</b> %s
                <b>Descrição:</b> %s
                <b>Data:</b> %s
                <b>Conta:</b> %s
                <b>Categoria:</b> %s
                
                Deseja confirmar e salvar?
                """.formatted(
                type,
                formatCurrency(parsedMessage.amount()),
                parsedMessage.description() != null ? escapeHtml(parsedMessage.description()) : "Não informada",
                formatDate(parsedMessage.date()),
                escapeHtml(accountName),
                escapeHtml(category)
        );
    }

    public String formatTransactionSuccess(TelegramIntentType intentType) {
        String label = intentType == TelegramIntentType.CREATE_EXPENSE ? "despesa" : "receita";

        return """
                ✅ <b>Transação registrada com sucesso!</b>
                
                Sua <b>%s</b> foi salva no sistema.
                """.formatted(label);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "Não informado";
        }

        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "Hoje";
        }

        return date.format(DATE_FORMATTER);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}