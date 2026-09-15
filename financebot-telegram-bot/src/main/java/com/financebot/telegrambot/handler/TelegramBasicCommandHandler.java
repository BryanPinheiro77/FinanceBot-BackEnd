package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.request.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.telegrambot.dto.response.FinancialCommitmentResponse;
import com.financebot.telegrambot.dto.response.TelegramLinkConfirmResponse;
import com.financebot.telegrambot.dto.response.UserProfileResponse;
import com.financebot.telegrambot.formatter.TelegramAccountMessageFormatter;
import com.financebot.telegrambot.service.TelegramPendingConfirmationService;
import com.financebot.telegrambot.service.TelegramPendingQueryService;
import com.financebot.telegrambot.support.TelegramBotErrorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class TelegramBasicCommandHandler {

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;
    private final TelegramPendingQueryService telegramPendingQueryService;
    private final TelegramAccountMessageFormatter telegramAccountMessageFormatter;
    private final TelegramBotErrorMapper telegramBotErrorMapper;

    public String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramAccountMessageFormatter.formatStartMessage(name);
    }

    public String handleHelp() {
        return telegramAccountMessageFormatter.formatHelpMessage()
                + "\n/alertas — consultar preferências\n/alertas ligar ou desligar — controlar notificações\n"
                + "/alertas semanal ligar|desligar e /alertas mensal ligar|desligar — controlar resumos";
    }

    public String handleAlerts(String text, Long telegramId) {
        String[] parts = text.toLowerCase(java.util.Locale.ROOT).split("\\s+");
        boolean valid = parts.length == 1
                || (parts.length == 2 && (parts[1].equals("ligar") || parts[1].equals("desligar")))
                || (parts.length == 3 && (parts[1].equals("semanal") || parts[1].equals("mensal"))
                    && (parts[2].equals("ligar") || parts[2].equals("desligar")));
        if (!valid) {
            return "Use /alertas, /alertas ligar|desligar ou /alertas semanal|mensal ligar|desligar.";
        }
        try {
            var preferences = financeBotApiClient.getAlertPreferences(telegramId);
            if (parts.length == 2) {
                boolean enabled = parts[1].equals("ligar");
                preferences = new com.financebot.telegrambot.alert.AlertPreferences(enabled, enabled, enabled);
            } else if (parts.length == 3) {
                boolean enabled = parts[2].equals("ligar");
                preferences = new com.financebot.telegrambot.alert.AlertPreferences(preferences.alerts(),
                        parts[1].equals("semanal") ? enabled : preferences.weeklySummary(),
                        parts[1].equals("mensal") ? enabled : preferences.monthlySummary());
            }
            if (parts.length > 1) {
                preferences = financeBotApiClient.updateAlertPreferences(telegramId, preferences);
            }
            return "Alertas: " + (preferences.alerts() ? "ligados" : "desligados")
                    + "; resumo semanal: " + (preferences.weeklySummary() ? "ligado" : "desligado")
                    + "; resumo mensal: " + (preferences.monthlySummary() ? "ligado" : "desligado") + ".";
        } catch (RestClientResponseException exception) {
            return telegramBotErrorMapper.mapDefaultBotErrors(exception);
        } catch (Exception exception) {
            return "Não foi possível consultar ou atualizar os alertas agora. Tente novamente.";
        }
    }

    public String handleGreeting(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);
        return telegramAccountMessageFormatter.formatGreetingMessage(name);
    }

    public String handleConnectionIntent() {
        return telegramAccountMessageFormatter.formatConnectInstructionsMessage();
    }

    public String handleConnect(String messageText, Long telegramId, String telegramUsername) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramAccountMessageFormatter.formatConnectCodeRequiredMessage();
        }

        String linkCode = parts[1].trim();

        try {
            TelegramLinkConfirmResponse response = financeBotApiClient.confirmTelegramLink(
                    new TelegramLinkConfirmRequest(linkCode, telegramId, telegramUsername)
            );

            return telegramAccountMessageFormatter.formatConnectSuccessMessage(response.message());
        } catch (RestClientResponseException e) {
            return telegramAccountMessageFormatter.formatConnectErrorMessage(e.getStatusCode().value());
        } catch (Exception e) {
            return telegramAccountMessageFormatter.formatGenericConnectFailureMessage();
        }
    }

    public String handleDisconnect(Long telegramId) {
        try {
            financeBotApiClient.disconnectTelegram(telegramId);
            telegramPendingConfirmationService.clearPending(telegramId);
            telegramPendingQueryService.clearPending(telegramId);

            return telegramAccountMessageFormatter.formatDisconnectSuccessMessage();
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramAccountMessageFormatter.formatGenericDisconnectFailureMessage();
        }
    }

    public String handleMe(Long telegramId) {
        try {
            UserProfileResponse response = financeBotApiClient.getMe(telegramId);

            return telegramAccountMessageFormatter.formatProfileMessage(
                    response.name(),
                    response.email(),
                    response.monthlyBaseIncome(),
                    response.telegramId() != null
            );
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramAccountMessageFormatter.formatGenericProfileFailureMessage();
        }
    }

    public String handleSetIncome(String messageText, Long telegramId) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return telegramAccountMessageFormatter.formatSetIncomeValueRequiredMessage();
        }

        try {
            BigDecimal income = parseBrazilianNumber(parts[1]);

            if (income.compareTo(BigDecimal.ZERO) <= 0) {
                return telegramAccountMessageFormatter.formatSetIncomeNonPositiveMessage();
            }

            UserProfileResponse response = financeBotApiClient.updateMonthlyBaseIncome(
                    telegramId,
                    new UpdateMonthlyBaseIncomeRequest(income)
            );

            return telegramAccountMessageFormatter.formatSetIncomeSuccessMessage(response.monthlyBaseIncome());
        } catch (NumberFormatException e) {
            return telegramAccountMessageFormatter.formatSetIncomeInvalidValueMessage();
        } catch (RestClientResponseException e) {
            return telegramBotErrorMapper.mapDefaultBotErrors(e);
        } catch (Exception e) {
            return telegramAccountMessageFormatter.formatGenericSetIncomeFailureMessage();
        }
    }

    public String handleReminder(String messageText, Long telegramId) {
        String[] parts = messageText.split("\\s+", 3);
        if (parts.length < 3) {
            return "Use: <code>/lembrete AAAA-MM-DD [dias] descrição</code>";
        }
        try {
            LocalDate date = LocalDate.parse(parts[1]);
            Integer daysBefore = 0;
            String description = parts[2].trim();
            String[] details = description.split("\\s+", 2);
            if (details[0].matches("-?\\d+")) {
                daysBefore = Integer.valueOf(details[0]);
                description = details.length == 2 ? details[1].trim() : "";
            }
            if (daysBefore < 0 || description.isBlank()) {
                return "Informe uma descrição e dias antes iguais ou maiores que zero.";
            }
            LocalDate notificationDate = date.minusDays(daysBefore);
            financeBotApiClient.createReminder(telegramId, description, notificationDate, 0);
            return "✅ <b>Lembrete criado!</b>\nVou avisar você em " + notificationDate + ".";
        } catch (DateTimeParseException | NumberFormatException exception) {
            return "Data ou quantidade de dias inválida. Use <code>/lembrete 2026-10-08 2 pagar aluguel</code>.";
        } catch (RestClientResponseException exception) {
            return telegramBotErrorMapper.mapDefaultBotErrors(exception);
        } catch (Exception exception) {
            return "Não foi possível criar o lembrete agora.";
        }
    }

    public String handleAnalysis(Long telegramId) {
        try {
            FinancialCommitmentResponse response = financeBotApiClient.getFinancialAnalysis(telegramId);

            return telegramAccountMessageFormatter.formatAnalysisMessage(
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

            return telegramAccountMessageFormatter.formatStatusMessage(
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
