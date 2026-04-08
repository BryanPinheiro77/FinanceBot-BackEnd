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

    public String formatStartMessage(String displayName) {
        String greeting = displayName != null && !displayName.isBlank()
                ? ", " + escapeHtml(displayName)
                : "";

        return """
                👋 <b>Bem-vindo%s ao Your Finance Assistant!</b>
                
                Eu posso ajudar você a conectar sua conta e acompanhar suas finanças direto pelo Telegram.
                
                <b>Comandos disponíveis:</b>
                <code>/start</code> ou <code>/iniciar</code> - Iniciar o bot
                <code>/help</code> ou <code>/ajuda</code> - Ver ajuda
                <code>/connect</code> ou <code>/conectar CODIGO</code> - Conectar sua conta
                <code>/me</code> ou <code>/perfil</code> - Ver seu perfil
                <code>/status</code> ou <code>/resumo</code> - Ver resumo da conta
                <code>/analysis</code> ou <code>/analise</code> - Ver análise financeira
                <code>/setincome</code> ou <code>/definirrenda VALOR</code> - Definir renda mensal base
                <code>/disconnect</code> ou <code>/desconectar</code> - Desconectar conta
                
                <b>Exemplos em linguagem natural:</b>
                • gastei 50 no mercado
                • recebi 1200 de salário
                • quanto gastei esse mês?
                • me dá a análise desse mês
                """.formatted(greeting);
    }

    public String formatHelpMessage() {
        return """
                ℹ️ <b>Comandos disponíveis</b>
                
                <code>/start</code> ou <code>/iniciar</code> - Iniciar o bot
                <code>/help</code> ou <code>/ajuda</code> - Ver ajuda
                <code>/connect</code> ou <code>/conectar CODIGO</code> - Conectar sua conta
                <code>/me</code> ou <code>/perfil</code> - Ver seu perfil
                <code>/status</code> ou <code>/resumo</code> - Ver resumo da conta
                <code>/analysis</code> ou <code>/analise</code> - Ver análise financeira
                <code>/setincome</code> ou <code>/definirrenda VALOR</code> - Definir renda mensal base
                <code>/disconnect</code> ou <code>/desconectar</code> - Desconectar conta
                
                <b>Exemplos com comandos:</b>
                <code>/connect FIN-ABC123</code>
                <code>/conectar FIN-ABC123</code>
                <code>/setincome 3500</code>
                <code>/definirrenda 3500</code>
                
                <b>Exemplos com linguagem natural:</b>
                • gastei 50 no mercado
                • paguei 120 de gasolina ontem
                • recebi 1500 de salário
                • quanto gastei esse mês?
                • quanto recebi esse mês?
                • me dá a análise desse mês
                """;
    }

    public String formatGreetingMessage(String displayName) {
        String greeting = displayName != null && !displayName.isBlank()
                ? ", " + escapeHtml(displayName)
                : "";

        return """
                👋 <b>Olá%s!</b>
                
                Eu sou seu assistente financeiro no Telegram.
                
                <b>Você pode usar:</b>
                <code>/help</code> ou <code>/ajuda</code> - Ver ajuda
                <code>/connect</code> ou <code>/conectar CODIGO</code> - Conectar sua conta
                <code>/me</code> ou <code>/perfil</code> - Ver seu perfil
                <code>/analysis</code> ou <code>/analise</code> - Ver análise financeira
                
                <b>Ou escrever naturalmente:</b>
                • gastei 50 no mercado
                • recebi 1200
                • quanto gastei esse mês?
                """.formatted(greeting);
    }

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