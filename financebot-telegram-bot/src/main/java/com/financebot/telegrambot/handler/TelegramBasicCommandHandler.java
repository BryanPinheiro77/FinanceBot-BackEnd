package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.request.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.telegrambot.dto.response.FinancialCommitmentResponse;
import com.financebot.telegrambot.dto.response.TelegramLinkConfirmResponse;
import com.financebot.telegrambot.dto.response.UserProfileResponse;
import com.financebot.telegrambot.formatter.TelegramMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TelegramBasicCommandHandler {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramMessageFormatter telegramMessageFormatter;
    private final TelegramBotErrorMapper telegramBotErrorMapper;

    public String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramMessageFormatter.formatStartMessage(name);
    }

    public String handleHelp() {
        return telegramMessageFormatter.formatHelpMessage();
    }

    public String handleGreeting(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramMessageFormatter.formatGreetingMessage(name);
    }

    public String handleConnectionIntent() {
        return telegramMessageFormatter.formatConnectInstructionsMessage();
    }

    public String handleConnect(String messageText, Long telegramId, String telegramUsername) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramMessageFormatter.formatConnectCodeRequiredMessage();
        }

        String linkCode = parts[1].trim();

        try {
            TelegramLinkConfirmResponse response = financeBotApiClient.confirmTelegramLink(
                    new TelegramLinkConfirmRequest(linkCode, telegramId, telegramUsername)
            );

            return telegramMessageFormatter.formatConnectSuccessMessage(response.message());
        } catch (RestClientResponseException e) {
            return telegramMessageFormatter.formatConnectErrorMessage(e.getStatusCode().value());
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericConnectFailureMessage();
        }
    }

    public String handleDisconnect(Long telegramId) {
        try {
            financeBotApiClient.disconnectTelegram(telegramId);
            telegramPendingConfirmationService.clearPending(telegramId);
            telegramPendingQueryService.clearPending(telegramId);

            return telegramMessageFormatter.formatDisconnectSuccessMessage();
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericDisconnectFailureMessage();
        }
    }

    public String handleMe(Long telegramId) {
        try {
            UserProfileResponse response = financeBotApiClient.getMe(telegramId);

            return telegramMessageFormatter.formatProfileMessage(
                    response.name(),
                    response.email(),
                    response.monthlyBaseIncome(),
                    response.telegramId() != null
            );
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericProfileFailureMessage();
        }
    }

    public String handleSetIncome(String messageText, Long telegramId) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramMessageFormatter.formatSetIncomeValueRequiredMessage();
        }

        try {
            BigDecimal income = parseBrazilianNumber(parts[1]);

            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                return telegramMessageFormatter.formatSetIncomeNonPositiveMessage();
            }

            UserProfileResponse response = financeBotApiClient.updateMonthlyBaseIncome(
                    telegramId,
                    new UpdateMonthlyBaseIncomeRequest(income)
            );

            return telegramMessageFormatter.formatSetIncomeSuccessMessage(response.monthlyBaseIncome());
        } catch (NumberFormatException e) {
            return telegramMessageFormatter.formatSetIncomeInvalidValueMessage();
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramMessageFormatter.formatGenericSetIncomeFailureMessage();
        }
    }

    public String handleAnalysis(Long telegramId) {
        try {
            FinancialCommitmentResponse response = financeBotApiClient.getFinancialAnalysis(telegramId);

            return telegramMessageFormatter.formatAnalysisMessage(
                    response.monthlyBaseIncome(),
                    response.monthlyIncomeReference(),
                    response.projectedRecurringIncomeNextMonth(),
                    response.projectedRecurringExpenseNextMonth(),
                    response.nextMonthProjectedIncome(),
                    response.nextMonthProjectedExpense(),
                    response.projectedNetNextMonth(),
                    response.commitmentPercentage(),
                    response.activeInstallmentCount(),
                    translateRiskLevel(response.riskLevel()),
                    response.message()
            );
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível gerar sua análise financeira agora.";
        }
    }

    public String handleStatus(Long telegramId) {
        try {
            UserProfileResponse profile = financeBotApiClient.getMe(telegramId);
            FinancialCommitmentResponse analysis = financeBotApiClient.getFinancialAnalysis(telegramId);

            return telegramMessageFormatter.formatStatusMessage(
                    profile.email(),
                    profile.monthlyBaseIncome(),
                    analysis.projectedNetNextMonth(),
                    translateRiskLevel(analysis.riskLevel())
            );
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível buscar o status da sua conta agora.";
        }
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

    private BigDecimal parseBrazilianNumber(String value) {
        String normalized = value.trim()
                .replace("R$", "")
                .replace(" ", "");

        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        }

        return new BigDecimal(normalized);
    }

    private String translateRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return "Não informado";
        }

        return switch (riskLevel.toUpperCase()) {
            case "LOW" -> "Baixo";
            case "MEDIUM" -> "Médio";
            case "HIGH" -> "Alto";
            default -> riskLevel;
        };
    }
}