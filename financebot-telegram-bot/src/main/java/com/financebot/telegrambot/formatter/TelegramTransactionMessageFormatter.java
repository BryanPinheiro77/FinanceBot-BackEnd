package com.financebot.telegrambot.formatter;

import com.financebot.telegrambot.dto.PendingTelegramTransaction;
import com.financebot.telegrambot.intent.TelegramIntentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formata previews e atualizações do fluxo de transações do Telegram. */
@Component
public class TelegramTransactionMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String formatTransactionPreview(PendingTelegramTransaction pendingTransaction, String accountName) {
        String type = pendingTransaction.intentType() == TelegramIntentType.CREATE_EXPENSE
                ? "despesa" : "receita";
        String category = pendingTransaction.categoryName() != null
                ? pendingTransaction.categoryName() : "Automática";

        return """
                💸 <b>Entendi esta %s:</b>

                <b>Valor:</b> %s
                <b>Descrição:</b> %s
                <b>Data:</b> %s
                <b>Conta:</b> %s
                <b>Categoria:</b> %s

                Deseja confirmar e salvar?
                """.formatted(type, formatCurrency(pendingTransaction.amount()),
                pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                formatDate(pendingTransaction.date()), escapeHtml(accountName), escapeHtml(category));
    }

    public String formatInstallmentTransactionPreview(PendingTelegramTransaction pendingTransaction, String accountName) {
        String category = pendingTransaction.categoryName() != null
                ? pendingTransaction.categoryName() : "Automática";

        return """
            💳 <b>Entendi este parcelamento:</b>

            <b>Valor total:</b> %s
            <b>Parcelas:</b> %s
            <b>Valor da parcela:</b> %s
            <b>Descrição:</b> %s
            <b>Primeira parcela:</b> %s
            <b>Conta:</b> %s
            <b>Categoria:</b> %s

            Deseja confirmar e salvar?
            """.formatted(formatCurrency(calculateEffectiveTotalAmount(pendingTransaction)),
                pendingTransaction.totalInstallments() != null ? pendingTransaction.totalInstallments() + "x" : "Não informada",
                formatCurrency(calculateInstallmentAmount(pendingTransaction)),
                pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                formatDate(pendingTransaction.date()), escapeHtml(accountName), escapeHtml(category));
    }

    public String formatExistingInstallmentTransactionPreview(PendingTelegramTransaction pendingTransaction, String accountName) {
        String category = pendingTransaction.categoryName() != null
                ? pendingTransaction.categoryName() : "Automática";
        Integer firstRemaining = pendingTransaction.firstRemainingInstallmentNumber();
        Integer paid = firstRemaining != null ? firstRemaining - 1 : null;
        String next = firstRemaining != null && pendingTransaction.totalInstallments() != null
                ? firstRemaining + "/" + pendingTransaction.totalInstallments() : "Não informada";

        return """
            💳 <b>Entendi este parcelamento em andamento:</b>

            <b>Valor total:</b> %s
            <b>Valor da parcela:</b> %s
            <b>Descrição:</b> %s
            <b>Parcelas:</b> %s
            <b>Parcelas pagas:</b> %s
            <b>Próxima parcela:</b> %s
            <b>Vencimento da próxima parcela:</b> %s
            <b>Conta:</b> %s
            <b>Categoria:</b> %s

            Deseja confirmar e salvar?
            """.formatted(formatCurrency(calculateEffectiveTotalAmount(pendingTransaction)),
                formatCurrency(calculateInstallmentAmount(pendingTransaction)),
                pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                pendingTransaction.totalInstallments() != null ? pendingTransaction.totalInstallments() + "x" : "Não informada",
                paid != null ? paid : "Não informada", next, formatDate(pendingTransaction.date()),
                escapeHtml(accountName), escapeHtml(category));
    }

    public String formatUpdatedPendingMessage(PendingTelegramTransaction pendingTransaction, String accountName) {
        if (pendingTransaction.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE) {
            String category = pendingTransaction.categoryName() != null ? pendingTransaction.categoryName() : "Automática";
            return """
        ✅ <b>Operação atualizada.</b>

        <b>Entendi este parcelamento:</b>

        <b>Valor total:</b> %s
        <b>Parcelas:</b> %s
        <b>Valor da parcela:</b> %s
        <b>Descrição:</b> %s
        <b>Primeira parcela:</b> %s
        <b>Conta:</b> %s
        <b>Categoria:</b> %s

        Deseja confirmar e salvar?
        """.formatted(formatCurrency(calculateEffectiveTotalAmount(pendingTransaction)),
                    pendingTransaction.totalInstallments() != null ? pendingTransaction.totalInstallments() + "x" : "Não informada",
                    formatCurrency(calculateInstallmentAmount(pendingTransaction)),
                    pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                    formatDate(pendingTransaction.date()), escapeHtml(accountName), escapeHtml(category));
        }

        if (pendingTransaction.intentType() == TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE) {
            String category = pendingTransaction.categoryName() != null ? pendingTransaction.categoryName() : "Automática";
            Integer firstRemaining = pendingTransaction.firstRemainingInstallmentNumber();
            Integer paid = firstRemaining != null ? firstRemaining - 1 : null;
            String next = firstRemaining != null && pendingTransaction.totalInstallments() != null
                    ? firstRemaining + "/" + pendingTransaction.totalInstallments() : "Não informada";
            return """
        ✅ <b>Operação atualizada.</b>

        <b>Entendi este parcelamento em andamento:</b>

        <b>Valor total:</b> %s
        <b>Valor da parcela:</b> %s
        <b>Descrição:</b> %s
        <b>Parcelas:</b> %s
        <b>Parcelas pagas:</b> %s
        <b>Próxima parcela:</b> %s
        <b>Vencimento da próxima parcela:</b> %s
        <b>Conta:</b> %s
        <b>Categoria:</b> %s

        Deseja confirmar e salvar?
        """.formatted(formatCurrency(calculateEffectiveTotalAmount(pendingTransaction)),
                    formatCurrency(calculateInstallmentAmount(pendingTransaction)),
                    pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                    pendingTransaction.totalInstallments() != null ? pendingTransaction.totalInstallments() + "x" : "Não informada",
                    paid != null ? paid : "Não informada", next, formatDate(pendingTransaction.date()),
                    escapeHtml(accountName), escapeHtml(category));
        }

        String type = pendingTransaction.intentType() == TelegramIntentType.CREATE_EXPENSE ? "despesa" : "receita";
        String category = pendingTransaction.categoryName() != null ? pendingTransaction.categoryName() : "Automática";
        return """
                ✅ <b>Operação atualizada.</b>

                <b>Entendi esta %s:</b>

                <b>Valor:</b> %s
                <b>Descrição:</b> %s
                <b>Data:</b> %s
                <b>Conta:</b> %s
                <b>Categoria:</b> %s

                Deseja confirmar e salvar?
                """.formatted(type, formatCurrency(pendingTransaction.amount()),
                pendingTransaction.description() != null ? escapeHtml(pendingTransaction.description()) : "Não informada",
                formatDate(pendingTransaction.date()), escapeHtml(accountName), escapeHtml(category));
    }

    private BigDecimal calculateEffectiveTotalAmount(PendingTelegramTransaction transaction) {
        if (transaction.totalInstallments() == null) return transaction.amount();
        if (transaction.amount() != null) return transaction.amount();
        if (transaction.monthlyAmount() != null) return transaction.monthlyAmount().multiply(BigDecimal.valueOf(transaction.totalInstallments()));
        return null;
    }

    private BigDecimal calculateInstallmentAmount(PendingTelegramTransaction transaction) {
        if (transaction.monthlyAmount() != null) return transaction.monthlyAmount();
        if (transaction.amount() == null || transaction.totalInstallments() == null) return null;
        return transaction.amount().divide(BigDecimal.valueOf(transaction.totalInstallments()), 2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "Não informado";
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Não informada" : DATE_FORMATTER.format(date);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
