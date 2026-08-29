package com.financebot.telegrambot.formatter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formata mensagens básicas e operações relacionadas à conta do usuário. */
@Component
public class TelegramAccountMessageFormatter {

    public String formatStartMessage(String displayName) {
        String greeting = displayName != null && !displayName.isBlank() ? ", " + escapeHtml(displayName) : "";
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
                • consigo comprar algo de 2000 parcelado em 12x?
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
                • se eu parcelar 2400 em 12x, cabe no meu orçamento?
                """;
    }

    public String formatGreetingMessage(String displayName) {
        String greeting = displayName != null && !displayName.isBlank() ? ", " + escapeHtml(displayName) : "";
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
                • consigo fazer uma compra de 3000 em 10x?
                """.formatted(greeting);
    }

    public String formatStatusMessage(String email, BigDecimal monthlyBaseIncome, BigDecimal projectedNetNextMonth, String riskLevel) {
        return """
            ✅ <b>Status da conta</b>
            
            <b>Conta conectada:</b> Sim
            <b>Email:</b> %s
            <b>Renda mensal base:</b> %s
            <b>Saldo projetado próximo mês:</b> %s
            <b>Nível de risco:</b> %s
            """.formatted(escapeHtml(defaultText(email)), formatCurrency(monthlyBaseIncome),
                formatCurrency(projectedNetNextMonth), escapeHtml(defaultText(riskLevel)));
    }

    public String formatAnalysisMessage(BigDecimal monthlyBaseIncome, BigDecimal monthlyIncomeReference,
            BigDecimal projectedRecurringIncomeNextMonth, BigDecimal projectedRecurringExpenseNextMonth,
            BigDecimal nextMonthProjectedIncome, BigDecimal nextMonthProjectedExpense, BigDecimal projectedNetNextMonth,
            BigDecimal commitmentPercentage, Long activeInstallmentCount, String riskLevel, String message) {
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
            """.formatted(formatCurrency(monthlyBaseIncome), formatCurrency(monthlyIncomeReference),
                formatCurrency(projectedRecurringIncomeNextMonth), formatCurrency(projectedRecurringExpenseNextMonth),
                formatCurrency(nextMonthProjectedIncome), formatCurrency(nextMonthProjectedExpense),
                formatCurrency(projectedNetNextMonth), commitmentPercentage != null ? commitmentPercentage : BigDecimal.ZERO,
                activeInstallmentCount != null ? activeInstallmentCount : 0L, escapeHtml(defaultText(riskLevel)),
                escapeHtml(defaultText(message)));
    }

    public String formatConnectCodeRequiredMessage() { return """
            🔗 <b>Você precisa enviar o código junto do comando.</b>
            
            <b>Exemplo:</b>
            <code>/connect FIN-ABC123</code>
            ou
            <code>/conectar FIN-ABC123</code>
            """; }

    public String formatConnectInstructionsMessage() { return """
            🔗 <b>Para conectar sua conta, gere um código no sistema e envie assim:</b>
            
            <code>/connect SEU_CODIGO</code>
            ou
            <code>/conectar SEU_CODIGO</code>
            
            <b>Exemplo:</b>
            <code>/connect FIN-ABC123</code>
            """; }

    public String formatConnectSuccessMessage(String message) { return escapeHtml(defaultText(message)); }

    public String formatDisconnectSuccessMessage() { return """
            ✅ <b>Sua conta do Telegram foi desconectada com sucesso.</b>
            
            Se quiser conectar novamente, gere um novo código no sistema e use:
            <code>/connect SEU_CODIGO</code>
            ou
            <code>/conectar SEU_CODIGO</code>
            """; }

    public String formatConnectErrorMessage(int statusCode) { return switch (statusCode) {
        case 400 -> "⚠️ <b>O código é inválido, expirou ou este Telegram já está vinculado a outra conta.</b>";
        case 401, 403 -> "⚠️ <b>O bot não tem permissão para concluir a conexão agora.</b>\nVerifique a configuração da API.";
        case 404 -> "⚠️ <b>Não encontrei uma conta para esse código.</b>\nGere um novo código no sistema.";
        default -> "⚠️ <b>Não foi possível conectar sua conta agora.</b>\nTente novamente em instantes.";
    }; }

    public String formatGenericConnectFailureMessage() { return """
            ⚠️ <b>Não foi possível conectar sua conta agora.</b>
            Verifique se o código está correto ou gere um novo no sistema.
            """; }
    public String formatGenericDisconnectFailureMessage() { return "⚠️ <b>Não foi possível desconectar sua conta agora.</b>"; }

    public String formatProfileMessage(String name, String email, BigDecimal monthlyBaseIncome, boolean telegramLinked) {
        return """
            👤 <b>Seu perfil</b>
            
            <b>Nome:</b> %s
            <b>Email:</b> %s
            <b>Renda mensal base:</b> %s
            <b>Telegram vinculado:</b> %s
            """.formatted(escapeHtml(defaultText(name)), escapeHtml(defaultText(email)),
                formatCurrency(monthlyBaseIncome), telegramLinked ? "Sim" : "Não");
    }

    public String formatSetIncomeValueRequiredMessage() { return """
            💰 <b>Você precisa informar um valor.</b>
            
            <b>Exemplo:</b>
            <code>/setincome 3500</code>
            ou
            <code>/definirrenda 3500</code>
            """; }
    public String formatSetIncomeInvalidValueMessage() { return """
            ⚠️ <b>Valor inválido.</b>
            
            <b>Exemplos válidos:</b>
            <code>/setincome 3500</code>
            <code>/setincome 3500,50</code>
            <code>/definirrenda 3500</code>
            <code>/definirrenda 3500,50</code>
            """; }
    public String formatSetIncomeNonPositiveMessage() { return "⚠️ <b>A renda mensal base deve ser maior que zero.</b>"; }
    public String formatSetIncomeSuccessMessage(BigDecimal value) { return """
            ✅ <b>Renda mensal base atualizada com sucesso!</b>
            
            <b>Novo valor:</b> %s
            """.formatted(formatCurrency(value)); }
    public String formatGenericProfileFailureMessage() { return "⚠️ <b>Não foi possível buscar seu perfil agora.</b>"; }
    public String formatGenericSetIncomeFailureMessage() { return "⚠️ <b>Não foi possível atualizar sua renda mensal base agora.</b>"; }

    private String defaultText(String value) { return value != null && !value.isBlank() ? value : "Não informado"; }
    private String formatCurrency(BigDecimal value) {
        return value == null ? "Não informado" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
