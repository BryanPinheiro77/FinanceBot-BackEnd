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

    public String formatStatusMessage(
            String email,
            BigDecimal monthlyBaseIncome,
            BigDecimal projectedNetNextMonth,
            String riskLevel
    ) {
        return """
            ✅ <b>Status da conta</b>
            
            <b>Conta conectada:</b> Sim
            <b>Email:</b> %s
            <b>Renda mensal base:</b> %s
            <b>Saldo projetado próximo mês:</b> %s
            <b>Nível de risco:</b> %s
            """.formatted(
                escapeHtml(defaultText(email)),
                formatCurrency(monthlyBaseIncome),
                formatCurrency(projectedNetNextMonth),
                escapeHtml(defaultText(riskLevel))
        );
    }

    public String formatAnalysisMessage(
            BigDecimal monthlyBaseIncome,
            BigDecimal monthlyIncomeReference,
            BigDecimal projectedRecurringIncomeNextMonth,
            BigDecimal projectedRecurringExpenseNextMonth,
            BigDecimal nextMonthProjectedIncome,
            BigDecimal nextMonthProjectedExpense,
            BigDecimal projectedNetNextMonth,
            BigDecimal commitmentPercentage,
            Long activeInstallmentCount,
            String riskLevel,
            String message
    ) {
        return """
            📊 <b>Análise financeira</b>
            
            <b>Renda mensal base:</b> %s
            <b>Renda de referência:</b> %s
            <b>Receita recorrente prevista:</b> %s
            <b>Despesa recorrente prevista:</b> %s
            <b>Receita projetada no próximo mês:</b> %s
            <b>Despesa projetada no próximo mês:</b> %s
            <b>Saldo projetado no próximo mês:</b> %s
            <b>Comprometimento:</b> %s%%
            <b>Grupos de parcelamento ativos:</b> %s
            <b>Nível de risco:</b> %s
            
            %s
            """.formatted(
                formatCurrency(monthlyBaseIncome),
                formatCurrency(monthlyIncomeReference),
                formatCurrency(projectedRecurringIncomeNextMonth),
                formatCurrency(projectedRecurringExpenseNextMonth),
                formatCurrency(nextMonthProjectedIncome),
                formatCurrency(nextMonthProjectedExpense),
                formatCurrency(projectedNetNextMonth),
                commitmentPercentage != null ? commitmentPercentage : BigDecimal.ZERO,
                activeInstallmentCount != null ? activeInstallmentCount : 0L,
                escapeHtml(defaultText(riskLevel)),
                escapeHtml(defaultText(message))
        );
    }

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

    public String formatTransactionSummary(
            String label,
            String complemento,
            BigDecimal totalAmount
    ) {
        return """
            📊 <b>Total %s%s</b>
            
            O total foi <b>%s</b>.
            """.formatted(
                escapeHtml(label),
                escapeHtml(complemento != null ? complemento : ""),
                formatCurrency(totalAmount)
        );
    }

    private String defaultText(String value) {
        return value != null && !value.isBlank() ? value : "Não informado";
    }

    public String formatTransactionSuccess(TelegramIntentType intentType) {
        String label = intentType == TelegramIntentType.CREATE_EXPENSE ? "despesa" : "receita";

        return """
                ✅ <b>Transação registrada com sucesso!</b>
                
                Sua <b>%s</b> foi salva no sistema.
                """.formatted(label);
    }

    public String formatConnectCodeRequiredMessage() {
        return """
            🔗 <b>Você precisa enviar o código junto do comando.</b>
            
            <b>Exemplo:</b>
            <code>/connect FIN-ABC123</code>
            ou
            <code>/conectar FIN-ABC123</code>
            """;
    }

    public String formatConnectInstructionsMessage() {
        return """
            🔗 <b>Para conectar sua conta, gere um código no sistema e envie assim:</b>
            
            <code>/connect SEU_CODIGO</code>
            ou
            <code>/conectar SEU_CODIGO</code>
            
            <b>Exemplo:</b>
            <code>/connect FIN-ABC123</code>
            """;
    }

    public String formatConnectSuccessMessage(String message) {
        return escapeHtml(defaultText(message));
    }

    public String formatDisconnectSuccessMessage() {
        return """
            ✅ <b>Sua conta do Telegram foi desconectada com sucesso.</b>
            
            Se quiser conectar novamente, gere um novo código no sistema e use:
            <code>/connect SEU_CODIGO</code>
            ou
            <code>/conectar SEU_CODIGO</code>
            """;
    }

    public String formatDefaultBotErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "⚠️ <b>A solicitação está inválida.</b>";
            case 404 -> "⚠️ <b>Não encontrei uma conta vinculada a este Telegram.</b>\nUse <code>/connect</code> ou <code>/conectar CODIGO</code>.";
            case 401, 403 -> "⚠️ <b>O bot não tem permissão para acessar esse recurso agora.</b>";
            case 500 -> "⚠️ <b>Ocorreu um erro interno ao processar sua solicitação.</b>";
            default -> "⚠️ <b>Ocorreu um erro ao processar sua solicitação.</b>";
        };
    }

    public String formatConnectErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "⚠️ <b>O código é inválido, expirou ou este Telegram já está vinculado a outra conta.</b>";
            case 401, 403 -> "⚠️ <b>O bot não tem permissão para concluir a conexão agora.</b>\nVerifique a configuração da API.";
            case 404 -> "⚠️ <b>Não encontrei uma conta para esse código.</b>\nGere um novo código no sistema.";
            default -> "⚠️ <b>Não foi possível conectar sua conta agora.</b>\nTente novamente em instantes.";
        };
    }

    public String formatGenericConnectFailureMessage() {
        return """
            ⚠️ <b>Não foi possível conectar sua conta agora.</b>
            Verifique se o código está correto ou gere um novo no sistema.
            """;
    }

    public String formatGenericDisconnectFailureMessage() {
        return "⚠️ <b>Não foi possível desconectar sua conta agora.</b>";
    }

    public String formatProfileMessage(
            String name,
            String email,
            BigDecimal monthlyBaseIncome,
            boolean telegramLinked
    ) {
        return """
            👤 <b>Seu perfil</b>
            
            <b>Nome:</b> %s
            <b>Email:</b> %s
            <b>Renda mensal base:</b> %s
            <b>Telegram vinculado:</b> %s
            """.formatted(
                escapeHtml(defaultText(name)),
                escapeHtml(defaultText(email)),
                formatCurrency(monthlyBaseIncome),
                telegramLinked ? "Sim" : "Não"
        );
    }

    public String formatSetIncomeValueRequiredMessage() {
        return """
            💰 <b>Você precisa informar um valor.</b>
            
            <b>Exemplo:</b>
            <code>/setincome 3500</code>
            ou
            <code>/definirrenda 3500</code>
            """;
    }

    public String formatSetIncomeInvalidValueMessage() {
        return """
            ⚠️ <b>Valor inválido.</b>
            
            <b>Exemplos válidos:</b>
            <code>/setincome 3500</code>
            <code>/setincome 3500,50</code>
            <code>/definirrenda 3500</code>
            <code>/definirrenda 3500,50</code>
            """;
    }

    public String formatInstallmentCountMessage(
            Long installmentCount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return """
            💳 <b>Parcelas no período</b>
            
            Você tem <b>%s</b> parcela(s) entre <b>%s</b> e <b>%s</b>.
            """.formatted(
                installmentCount != null ? installmentCount : 0L,
                formatDate(startDate),
                formatDate(endDate)
        );
    }

    public String formatActiveInstallmentsMessage(Long activeInstallmentGroupCount) {
        return """
            💳 <b>Parcelamentos ativos</b>
            
            Você tem <b>%s</b> parcelamento(s) ativo(s).
            """.formatted(activeInstallmentGroupCount != null ? activeInstallmentGroupCount : 0L);
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

    public String formatRemainingInstallmentsMessage(
            String description,
            Integer remainingInstallments,
            Integer nextInstallmentNumber,
            Integer totalInstallments
    ) {
        return """
            💳 <b>Parcelas restantes</b>
            
            <b>Descrição:</b> %s
            <b>Próxima parcela:</b> %s/%s
            <b>Faltam:</b> %s parcela(s)
            """.formatted(
                escapeHtml(defaultText(description)),
                nextInstallmentNumber != null ? nextInstallmentNumber : 0,
                totalInstallments != null ? totalInstallments : 0,
                remainingInstallments != null ? remainingInstallments : 0
        );
    }

    public String formatInstallmentEndDateMessage(
            String description,
            LocalDate endDate
    ) {
        return """
            💳 <b>Fim do parcelamento</b>
            
            <b>Descrição:</b> %s
            <b>Última parcela:</b> %s
            """.formatted(
                escapeHtml(defaultText(description)),
                formatDate(endDate)
        );
    }

    public String formatSetIncomeNonPositiveMessage() {
        return "⚠️ <b>A renda mensal base deve ser maior que zero.</b>";
    }

    public String formatSetIncomeSuccessMessage(BigDecimal monthlyBaseIncome) {
        return """
            ✅ <b>Renda mensal base atualizada com sucesso!</b>
            
            <b>Novo valor:</b> %s
            """.formatted(formatCurrency(monthlyBaseIncome));
    }

    public String formatGenericProfileFailureMessage() {
        return "⚠️ <b>Não foi possível buscar seu perfil agora.</b>";
    }

    public String formatGenericSetIncomeFailureMessage() {
        return "⚠️ <b>Não foi possível atualizar sua renda mensal base agora.</b>";
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
