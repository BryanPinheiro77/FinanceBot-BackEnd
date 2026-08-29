package com.financebot.telegrambot.formatter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formata respostas de consultas financeiras do Telegram. */
@Component
public class TelegramQueryMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String formatMonthExpenseSummary(BigDecimal totalAmount) {
        return """
            💸 <b>Total gasto no mês</b>
            
            Você gastou <b>%s</b> neste mês.
            """.formatted(formatCurrency(totalAmount));
    }

    public String formatMonthIncomeSummary(BigDecimal totalAmount) {
        return """
            💰 <b>Total recebido no mês</b>
            
            Você recebeu <b>%s</b> neste mês.
            """.formatted(formatCurrency(totalAmount));
    }

    public String formatTransactionSummary(String label, String complemento, BigDecimal totalAmount) {
        return """
            📊 <b>Total %s%s</b>
            
            O total foi <b>%s</b>.
            """.formatted(escapeHtml(label), escapeHtml(complemento != null ? complemento : ""), formatCurrency(totalAmount));
    }

    public String formatInstallmentPurchaseCapacityMessage(
            BigDecimal totalAmount, Integer totalInstallments, BigDecimal estimatedInstallmentAmount,
            String analysisResult, String observation
    ) {
        return """
            💳 <b>Análise de compra parcelada</b>
            
            <b>Valor total:</b> %s
            <b>Parcelas:</b> %s
            <b>Valor estimado por parcela:</b> %s
            <b>Resultado:</b> %s
            
            %s
            """.formatted(formatCurrency(totalAmount),
                totalInstallments != null ? totalInstallments + "x" : "Não informado",
                formatCurrency(estimatedInstallmentAmount), escapeHtml(formatAnalysisResult(analysisResult)),
                escapeHtml(defaultText(observation)));
    }

    public String formatInstallmentCountMessage(Long count, LocalDate startDate, LocalDate endDate) {
        return """
            💳 <b>Parcelas no período</b>
            
            Você tem <b>%s</b> parcela(s) entre <b>%s</b> e <b>%s</b>.
            """.formatted(count != null ? count : 0L, formatDate(startDate), formatDate(endDate));
    }

    public String formatActiveInstallmentsMessage(Long count) {
        return """
            💳 <b>Parcelamentos ativos</b>
            
            Você tem <b>%s</b> parcelamento(s) ativo(s).
            """.formatted(count != null ? count : 0L);
    }

    public String formatNoActiveInstallmentsMessage() {
        return """
            💳 <b>Parcelamentos</b>
            
            Você não tem parcelamentos ativos no momento.
            """;
    }

    public String formatMultipleActiveInstallmentsMessage() {
        return """
            💳 <b>Parcelamentos</b>
            
            Você tem mais de um parcelamento ativo.
            Me diga qual deles deseja consultar.
            """;
    }

    public String formatInstallmentNotFoundMessage(String target) {
        return """
            💳 <b>Parcelamentos</b>
            
            Não encontrei um parcelamento ativo para <b>%s</b>.
            """.formatted(escapeHtml(defaultText(target)));
    }

    public String formatRemainingInstallmentsMessage(String description, LocalDate currentDueDate,
            Integer currentInstallmentNumber, LocalDate nextDueDate, Integer remainingInstallments,
            Integer nextInstallmentNumber, Integer totalInstallments) {
        String current = currentInstallmentNumber != null && totalInstallments != null
                ? currentInstallmentNumber + "/" + totalInstallments : "Não iniciada";
        String nextDate = nextDueDate != null ? formatDate(nextDueDate) : "Parcelamento encerrado";
        String next = nextInstallmentNumber != null && totalInstallments != null
                ? nextInstallmentNumber + "/" + totalInstallments : "Encerrada";
        String remaining = remainingInstallments != null && remainingInstallments > 0
                ? remainingInstallments + " parcela(s)" : "Nenhuma parcela restante";
        return """
            💳 <b>Parcelas restantes</b>
            
            <b>Descrição:</b> %s
            <b>Parcela atual:</b> %s
            <b>Próximo vencimento:</b> %s
            <b>Próxima parcela:</b> %s
            <b>Faltam:</b> %s
            """.formatted(escapeHtml(defaultText(description)), current, nextDate, next, remaining);
    }

    public String formatInstallmentEndDateMessage(String description, LocalDate endDate) {
        return """
            💳 <b>Fim do parcelamento</b>
            
            <b>Descrição:</b> %s
            <b>Última parcela:</b> %s
            """.formatted(escapeHtml(defaultText(description)), formatDate(endDate));
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "Não informado";
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "Não informada" : DATE_FORMATTER.format(value);
    }

    private String defaultText(String value) {
        return value != null && !value.isBlank() ? value : "Não informado";
    }

    private String formatAnalysisResult(String value) {
        if (value == null || value.isBlank()) return "Não informado";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "VIAVEL" -> "Viável";
            case "ALERTA" -> "Alerta";
            case "NAO_VIAVEL" -> "Não viável";
            default -> value;
        };
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
