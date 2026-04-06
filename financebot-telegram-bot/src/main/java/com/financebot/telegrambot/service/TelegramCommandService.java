package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.FinancialCommitmentResponse;
import com.financebot.telegrambot.dto.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.TelegramLinkConfirmResponse;
import com.financebot.telegrambot.dto.UpdateMonthlyBaseIncomeRequest;
import com.financebot.telegrambot.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    private final FinanceBotApiClient financeBotApiClient;

    public String handleMessage(
            String messageText,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        if (messageText == null || messageText.isBlank()) {
            return "Não consegui entender sua mensagem. Tente /start, /help ou /connect CODIGO.";
        }

        String normalizedMessage = messageText.trim();

        if (normalizedMessage.startsWith("/start")) {
            return handleStart(telegramFirstName, telegramUsername);
        }

        if (normalizedMessage.startsWith("/help")) {
            return handleHelp();
        }

        if (normalizedMessage.startsWith("/connect")) {
            return handleConnect(normalizedMessage, telegramId, telegramUsername);
        }

        if (normalizedMessage.startsWith("/me")) {
            return handleMe(telegramId);
        }

        if (normalizedMessage.startsWith("/disconnect")) {
            return handleDisconnect(telegramId);
        }

        if (normalizedMessage.startsWith("/setincome")) {
            return handleSetIncome(normalizedMessage, telegramId);
        }

        if (normalizedMessage.startsWith("/analysis")) {
            return handleAnalysis(telegramId);
        }

        if (normalizedMessage.startsWith("/status")) {
            return handleStatus(telegramId);
        }

        if (containsGreeting(normalizedMessage)) {
            return handleGreeting(telegramFirstName, telegramUsername);
        }

        if (looksLikeConnectionIntent(normalizedMessage)) {
            return """
                    Para conectar sua conta, gere um código no sistema e envie aqui assim:
                    
                    /connect SEU_CODIGO
                    
                    Exemplo:
                    /connect FIN-ABC123
                    """;
        }

        return """
                Não reconheci sua mensagem.
                
                Tente um destes comandos:
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                /me - Ver seu perfil
                /status - Ver resumo da conta
                /analysis - Ver análise financeira
                /setincome VALOR - Definir renda mensal base
                /disconnect - Desconectar conta
                """;
    }

    private String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);

        return """
                Bem-vindo%s ao Your Finance Assistant!
                
                Eu posso ajudar você a conectar sua conta e acompanhar suas finanças direto pelo Telegram.
                
                Comandos disponíveis:
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                /me - Ver seu perfil
                /status - Ver resumo da conta
                /analysis - Ver análise financeira
                /setincome VALOR - Definir renda mensal base
                /disconnect - Desconectar conta
                """.formatted(name != null ? ", " + name : "");
    }

    private String handleHelp() {
        return """
                Comandos disponíveis:
                
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                /me - Ver seu perfil
                /status - Ver resumo da conta
                /analysis - Ver análise financeira
                /setincome VALOR - Definir renda mensal base
                /disconnect - Desconectar conta
                
                Exemplos:
                /connect FIN-ABC123
                /setincome 3500
                """;
    }

    private String handleGreeting(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);

        return """
                Olá%s! 👋
                
                Eu sou seu assistente financeiro no Telegram.
                Você pode usar:
                
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                /me - Ver seu perfil
                /analysis - Ver análise financeira
                """.formatted(name != null ? ", " + name : "");
    }

    private String handleConnect(String messageText, Long telegramId, String telegramUsername) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return """
                    Você precisa enviar o código junto do comando.
                    
                    Exemplo:
                    /connect FIN-ABC123
                    """;
        }

        String linkCode = parts[1].trim();

        try {
            TelegramLinkConfirmResponse response = financeBotApiClient.confirmTelegramLink(
                    new TelegramLinkConfirmRequest(linkCode, telegramId, telegramUsername)
            );

            return response.message();
        } catch (RestClientResponseException e) {
            return switch (e.getStatusCode().value()) {
                case 400 -> "O código é inválido, expirou ou este Telegram já está vinculado a outra conta.";
                case 401, 403 -> "O bot não tem permissão para concluir a conexão agora. Verifique a configuração da API.";
                case 404 -> "Não encontrei uma conta para esse código. Gere um novo código no sistema.";
                default -> "Não foi possível conectar sua conta agora. Tente novamente em instantes.";
            };
        } catch (Exception e) {
            return """
                    Não foi possível conectar sua conta agora.
                    Verifique se o código está correto ou gere um novo no sistema.
                    """;
        }
    }

    private String handleMe(Long telegramId) {
        try {
            UserProfileResponse response = financeBotApiClient.getMe(telegramId);

            return """
                    👤 Seu perfil
                    
                    Nome: %s
                    Email: %s
                    Renda mensal base: %s
                    Telegram vinculado: %s
                    """.formatted(
                    defaultText(response.name()),
                    defaultText(response.email()),
                    formatCurrency(response.monthlyBaseIncome()),
                    response.telegramId() != null ? "Sim" : "Não"
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível buscar seu perfil agora.";
        }
    }

    private String handleDisconnect(Long telegramId) {
        try {
            financeBotApiClient.disconnectTelegram(telegramId);
            return """
                    ✅ Sua conta do Telegram foi desconectada com sucesso.
                    
                    Se quiser conectar novamente depois, gere um novo código no sistema e use:
                    /connect SEU_CODIGO
                    """;
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível desconectar sua conta agora.";
        }
    }

    private String handleSetIncome(String messageText, Long telegramId) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return """
                    Você precisa informar um valor.
                    
                    Exemplo:
                    /setincome 3500
                    """;
        }

        try {
            BigDecimal income = parseBrazilianNumber(parts[1]);

            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                return "A renda mensal base deve ser maior que zero.";
            }

            UserProfileResponse response = financeBotApiClient.updateMonthlyBaseIncome(
                    telegramId,
                    new UpdateMonthlyBaseIncomeRequest(income)
            );

            return "✅ Renda mensal base atualizada para " + formatCurrency(response.monthlyBaseIncome());
        } catch (NumberFormatException e) {
            return """
                    Valor inválido.
                    
                    Exemplos válidos:
                    /setincome 3500
                    /setincome 3500,50
                    /setincome 4200.75
                    """;
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível atualizar sua renda mensal base agora.";
        }
    }

    private String handleAnalysis(Long telegramId) {
        try {
            FinancialCommitmentResponse response = financeBotApiClient.getFinancialAnalysis(telegramId);

            return """
                    📊 Análise financeira
                    
                    Renda mensal base: %s
                    Receita recorrente prevista: %s
                    Despesa recorrente prevista: %s
                    Receita projetada no próximo mês: %s
                    Despesa projetada no próximo mês: %s
                    Saldo projetado no próximo mês: %s
                    Comprometimento: %s%%
                    Grupos de parcelamento ativos: %s
                    Nível de risco: %s
                    """.formatted(
                    formatCurrency(response.monthlyBaseIncome()),
                    formatCurrency(response.projectedRecurringIncomeNextMonth()),
                    formatCurrency(response.projectedRecurringExpenseNextMonth()),
                    formatCurrency(response.nextMonthProjectedIncome()),
                    formatCurrency(response.nextMonthProjectedExpense()),
                    formatCurrency(response.projectedNetNextMonth()),
                    response.commitmentPercentage() != null ? response.commitmentPercentage() : BigDecimal.ZERO,
                    response.activeInstallmentGroups() != null ? response.activeInstallmentGroups() : 0,
                    defaultText(response.riskLevel())
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível gerar sua análise financeira agora.";
        }
    }

    private String handleStatus(Long telegramId) {
        try {
            UserProfileResponse profile = financeBotApiClient.getMe(telegramId);
            FinancialCommitmentResponse analysis = financeBotApiClient.getFinancialAnalysis(telegramId);

            return """
                    ✅ Status da conta
                    
                    Conta conectada: Sim
                    Email: %s
                    Renda mensal base: %s
                    Saldo projetado próximo mês: %s
                    Nível de risco: %s
                    """.formatted(
                    defaultText(profile.email()),
                    formatCurrency(profile.monthlyBaseIncome()),
                    formatCurrency(analysis.projectedNetNextMonth()),
                    defaultText(analysis.riskLevel())
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível buscar o status da sua conta agora.";
        }
    }

    private String mapDefaultBotErrors(RestClientResponseException e) {
        return switch (e.getStatusCode().value()) {
            case 400 -> "A solicitação está inválida.";
            case 404 -> "Não encontrei uma conta vinculada a este Telegram. Use /connect CODIGO.";
            case 401, 403 -> "O bot não tem permissão para acessar esse recurso agora.";
            default -> "Ocorreu um erro ao processar sua solicitação.";
        };
    }

    private String resolveDisplayName(String telegramFirstName, String telegramUsername) {
        if (telegramFirstName != null && !telegramFirstName.isBlank()) {
            return telegramFirstName.trim();
        }

        if (telegramUsername != null && !telegramUsername.isBlank()) {
            return "@" + telegramUsername.trim();
        }

        return null;
    }

    private boolean containsGreeting(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("oi")
                || lower.equals("olá")
                || lower.equals("ola")
                || lower.equals("bom dia")
                || lower.equals("boa tarde")
                || lower.equals("boa noite");
    }

    private boolean looksLikeConnectionIntent(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.contains("conectar")
                || lower.contains("vincular")
                || lower.contains("ligar conta")
                || lower.contains("linkar")
                || lower.contains("telegram");
    }

    private String defaultText(String value) {
        return value != null && !value.isBlank() ? value : "Não informado";
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "Não informado";
        }

        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private BigDecimal parseBrazilianNumber(String value) {
        String normalized = value.trim()
                .replace("R$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(normalized);
    }
}