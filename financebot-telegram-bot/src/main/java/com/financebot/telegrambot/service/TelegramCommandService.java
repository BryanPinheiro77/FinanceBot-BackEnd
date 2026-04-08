package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.*;
import com.financebot.telegrambot.intent.TelegramIntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramCommandService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinanceBotApiClient financeBotApiClient;
    private final TelegramIntentService telegramIntentService;
    private final TelegramPendingConfirmationService telegramPendingConfirmationService;

    public String handleMessage(
            String messageText,
            Long telegramId,
            String telegramUsername,
            String telegramFirstName
    ) {
        if (messageText == null || messageText.isBlank()) {
            return "Não consegui entender sua mensagem. Tente /start, /iniciar, /help ou /ajuda.";
        }

        String normalizedMessage = messageText.trim();

        if (startsWithCommand(normalizedMessage, "/start", "/iniciar")) {
            return handleStart(telegramFirstName, telegramUsername);
        }

        if (startsWithCommand(normalizedMessage, "/help", "/ajuda")) {
            return handleHelp();
        }

        if (startsWithCommand(normalizedMessage, "/connect", "/conectar")) {
            return handleConnect(normalizedMessage, telegramId, telegramUsername);
        }

        if (startsWithCommand(normalizedMessage, "/me", "/perfil")) {
            return handleMe(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/disconnect", "/desconectar")) {
            return handleDisconnect(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/setincome", "/definirrenda")) {
            return handleSetIncome(normalizedMessage, telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/analysis", "/analise")) {
            return handleAnalysis(telegramId);
        }

        if (startsWithCommand(normalizedMessage, "/status", "/resumo")) {
            return handleStatus(telegramId);
        }

        if (containsGreeting(normalizedMessage)) {
            return handleGreeting(telegramFirstName, telegramUsername);
        }

        if (looksLikeConnectionIntent(normalizedMessage)) {
            return """
                    Para conectar sua conta, gere um código no sistema e envie assim:
                    
                    /connect SEU_CODIGO
                    ou
                    /conectar SEU_CODIGO
                    
                    Exemplo:
                    /connect FIN-ABC123
                    """;
        }

        if (isConfirmationMessage(normalizedMessage)) {
            return handleConfirmation(telegramId);
        }

        if (isCancellationMessage(normalizedMessage)) {
            return handleCancellation(telegramId);
        }

        if (telegramPendingConfirmationService.hasPending(telegramId) && looksLikeEditMessage(normalizedMessage)) {
            return handlePendingEdit(telegramId, normalizedMessage);
        }

        ParsedTelegramMessage parsedMessage = telegramIntentService.parse(normalizedMessage);

        if (parsedMessage.intentType() != null && parsedMessage.intentType().name().startsWith("QUERY_")) {
            return handleNaturalLanguageQuery(parsedMessage, telegramId);
        }

        if (parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_INCOME) {
            return handleNaturalLanguageTransactionPreview(telegramId, parsedMessage);
        }

        return """
                Não reconheci sua mensagem.
                
                Você pode usar comandos:
                /start ou /iniciar
                /help ou /ajuda
                /connect ou /conectar CODIGO
                /me ou /perfil
                /status ou /resumo
                /analysis ou /analise
                /setincome ou /definirrenda VALOR
                /disconnect ou /desconectar
                
                Ou pode escrever naturalmente, por exemplo:
                - gastei 50 no mercado
                - recebi 1200 de salário
                - quanto gastei esse mês?
                - me dá a análise desse mês
                """;
    }

    private String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);

        return """
                Bem-vindo%s ao Your Finance Assistant!
                
                Eu posso ajudar você a conectar sua conta e acompanhar suas finanças direto pelo Telegram.
                
                Você pode usar comandos:
                /start ou /iniciar - Iniciar o bot
                /help ou /ajuda - Ver ajuda
                /connect ou /conectar CODIGO - Conectar sua conta
                /me ou /perfil - Ver seu perfil
                /status ou /resumo - Ver resumo da conta
                /analysis ou /analise - Ver análise financeira
                /setincome ou /definirrenda VALOR - Definir renda mensal base
                /disconnect ou /desconectar - Desconectar conta
                
                Ou pode escrever naturalmente, por exemplo:
                - gastei 50 no mercado
                - recebi 1200 de salário
                - quanto gastei esse mês?
                - me dá a análise desse mês
                """.formatted(name != null ? ", " + name : "");
    }

    private String handleHelp() {
        return """
                Comandos disponíveis:
                
                /start ou /iniciar - Iniciar o bot
                /help ou /ajuda - Ver ajuda
                /connect ou /conectar CODIGO - Conectar sua conta
                /me ou /perfil - Ver seu perfil
                /status ou /resumo - Ver resumo da conta
                /analysis ou /analise - Ver análise financeira
                /setincome ou /definirrenda VALOR - Definir renda mensal base
                /disconnect ou /desconectar - Desconectar conta
                
                Exemplos com comandos:
                /connect FIN-ABC123
                /conectar FIN-ABC123
                /setincome 3500
                /definirrenda 3500
                
                Exemplos com linguagem natural:
                - gastei 50 no mercado
                - paguei 120 de gasolina ontem
                - recebi 1500 de salário
                - quanto gastei esse mês?
                - quanto recebi esse mês?
                - me dá a análise desse mês
                """;
    }

    private String handleGreeting(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);

        return """
                Olá%s! 👋
                
                Eu sou seu assistente financeiro no Telegram.
                
                Você pode usar comandos:
                /help ou /ajuda - Ver ajuda
                /connect ou /conectar CODIGO - Conectar sua conta
                /me ou /perfil - Ver seu perfil
                /analysis ou /analise - Ver análise financeira
                
                Ou escrever naturalmente:
                - gastei 50 no mercado
                - recebi 1200
                - quanto gastei esse mês?
                """.formatted(name != null ? ", " + name : "");
    }

    private String handleConnect(String messageText, Long telegramId, String telegramUsername) {
        String[] parts = messageText.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return """
                    Você precisa enviar o código junto do comando.
                    
                    Exemplo:
                    /connect FIN-ABC123
                    ou
                    /conectar FIN-ABC123
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
            telegramPendingConfirmationService.clearPending(telegramId);

            return """
                    ✅ Sua conta do Telegram foi desconectada com sucesso.
                    
                    Se quiser conectar novamente, gere um novo código no sistema e use:
                    /connect SEU_CODIGO
                    ou
                    /conectar SEU_CODIGO
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
                    ou
                    /definirrenda 3500
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
                    /definirrenda 3500
                    /definirrenda 3500,50
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
                    Renda de referência: %s
                    Receita recorrente prevista: %s
                    Despesa recorrente prevista: %s
                    Receita projetada no próximo mês: %s
                    Despesa projetada no próximo mês: %s
                    Saldo projetado no próximo mês: %s
                    Comprometimento: %s%%
                    Grupos de parcelamento ativos: %s
                    Nível de risco: %s
                    
                    %s
                    """.formatted(
                    formatCurrency(response.monthlyBaseIncome()),
                    formatCurrency(response.monthlyIncomeReference()),
                    formatCurrency(response.projectedRecurringIncomeNextMonth()),
                    formatCurrency(response.projectedRecurringExpenseNextMonth()),
                    formatCurrency(response.nextMonthProjectedIncome()),
                    formatCurrency(response.nextMonthProjectedExpense()),
                    formatCurrency(response.projectedNetNextMonth()),
                    response.commitmentPercentage() != null ? response.commitmentPercentage() : BigDecimal.ZERO,
                    response.activeInstallmentCount() != null ? response.activeInstallmentCount() : 0L,
                    translateRiskLevel(response.riskLevel()),
                    defaultText(response.message())
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
                    translateRiskLevel(analysis.riskLevel())
            );
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível buscar o status da sua conta agora.";
        }
    }

    private String handleNaturalLanguageTransactionPreview(Long telegramId, ParsedTelegramMessage parsedMessage) {
        if (parsedMessage.amount() == null) {
            return """
                Entendi a intenção, mas não consegui identificar o valor.
                
                Exemplos:
                - gastei 50 no mercado
                - paguei 120 de gasolina
                - recebi 1500 de salário
                """;
        }

        telegramPendingConfirmationService.savePending(telegramId, parsedMessage);

        String type = parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE ? "despesa" : "receita";

        String conta = resolvePreviewAccountName(parsedMessage, telegramId);

        String categoria = parsedMessage.categoryName() != null
                ? parsedMessage.categoryName()
                : "automática";

        return """
            Entendi esta %s:
            
            Valor: %s
            Descrição: %s
            Data: %s
            Conta: %s
            Categoria: %s
            
            Deseja confirmar e salvar?
            """.formatted(
                type,
                formatCurrency(parsedMessage.amount()),
                parsedMessage.description() != null ? parsedMessage.description() : "Não informada",
                formatDate(parsedMessage.date()),
                conta,
                categoria
        );
    }

    private String handleNaturalLanguageQuery(ParsedTelegramMessage parsedMessage, Long telegramId) {
        try {
            return switch (parsedMessage.intentType()) {
                case QUERY_MONTH_EXPENSE_TOTAL -> {
                    MonthlyAmountSummaryResponse response = financeBotApiClient.getCurrentMonthExpenseSummary(telegramId);

                    yield """
                        💸 Total gasto no mês
                        
                        Você gastou %s neste mês.
                        """.formatted(formatCurrency(response.totalAmount()));
                }
                case QUERY_MONTH_INCOME_TOTAL -> {
                    MonthlyAmountSummaryResponse response = financeBotApiClient.getCurrentMonthIncomeSummary(telegramId);

                    yield """
                        💰 Total recebido no mês
                        
                        Você recebeu %s neste mês.
                        """.formatted(formatCurrency(response.totalAmount()));
                }
                case QUERY_MONTH_ANALYSIS -> handleAnalysis(telegramId);

                case QUERY_TRANSACTION_TOTAL -> {
                    String type = parsedMessage.originalMessage().toLowerCase().contains("recebi")
                            || parsedMessage.originalMessage().toLowerCase().contains("entrou")
                            ? "INCOME"
                            : "EXPENSE";

                    TelegramTransactionSummaryResponse response = financeBotApiClient.getTransactionSummary(
                            new TelegramTransactionSummaryRequest(
                                    telegramId,
                                    type,
                                    parsedMessage.categoryName(),
                                    parsedMessage.accountName(),
                                    parsedMessage.startDate(),
                                    parsedMessage.endDate()
                            )
                    );

                    String label = "EXPENSE".equals(type) ? "gasto" : "recebido";

                    String complemento = response.categoryName() != null
                            ? " em " + response.categoryName()
                            : response.accountName() != null
                            ? " na conta " + response.accountName()
                            : "";

                    yield """
                        📊 Total %s%s
                        
                        O total foi %s.
                        """.formatted(
                            label,
                            complemento,
                            formatCurrency(response.totalAmount())
                    );
                }

                default -> "Não consegui interpretar sua consulta.";
            };
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return "Não foi possível consultar essas informações agora.";
        }
    }

    private String handleConfirmation(Long telegramId) {
        ParsedTelegramMessage pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para confirmar.";
        }

        try {
            CreateTransactionFromTelegramRequest request =
                    new CreateTransactionFromTelegramRequest(
                            telegramId,
                            mapIntentToTransactionType(pending.intentType()),
                            pending.amount(),
                            pending.description(),
                            pending.date(),
                            pending.categoryName(),
                            pending.accountName()
                    );

            financeBotApiClient.createTransaction(request);
            telegramPendingConfirmationService.clearPending(telegramId);

            String transactionLabel = pending.intentType() == TelegramIntentType.CREATE_EXPENSE
                    ? "despesa"
                    : "receita";

            return """
                ✅ Transação registrada com sucesso!
                
                Sua %s foi salva no sistema.
                """.formatted(transactionLabel);
        } catch (RestClientResponseException e) {
            return mapDefaultBotErrors(e);
        } catch (Exception e) {
            return """
                Não foi possível salvar sua transação agora.
                Você pode tentar confirmar novamente em instantes.
                """;
        }
    }

    private String handleCancellation(Long telegramId) {
        if (!telegramPendingConfirmationService.hasPending(telegramId)) {
            return "Não há nenhuma operação pendente para cancelar.";
        }

        telegramPendingConfirmationService.clearPending(telegramId);

        return "❌ Operação cancelada com sucesso.";
    }

    private String handlePendingEdit(Long telegramId, String messageText) {
        ParsedTelegramMessage pending = telegramPendingConfirmationService.getPending(telegramId);

        if (pending == null) {
            return "Não há nenhuma operação pendente para editar.";
        }

        String lower = normalizeText(messageText);

        BigDecimal amount = pending.amount();
        String description = pending.description();
        LocalDate date = pending.date();
        String categoryName = pending.categoryName();
        String accountName = pending.accountName();

        boolean changed = false;

        if (containsAmountEditHint(lower)) {
            BigDecimal newAmount = extractAmountFromEdit(messageText);

            if (newAmount != null && newAmount.compareTo(BigDecimal.ZERO) > 0) {
                amount = newAmount;
                changed = true;
            }
        }

        if (containsDescriptionEditHint(lower)) {
            String newDescription = extractDescriptionFromEdit(messageText);

            if (newDescription != null && !newDescription.isBlank()) {
                description = newDescription;
                changed = true;
            }
        }

        if (containsCategoryEditHint(lower)) {
            String newCategory = extractCategoryFromEdit(messageText);

            if (newCategory != null && !newCategory.isBlank()) {
                categoryName = newCategory;
                changed = true;
            }
        }

        if (containsDateEditHint(lower)) {
            LocalDate newDate = extractDateFromEdit(messageText);

            if (newDate != null) {
                date = newDate;
                changed = true;
            }
        }

        if (containsAccountEditHint(lower)) {
            String newAccount = extractAccountFromEdit(messageText);

            if (newAccount != null && !newAccount.isBlank()) {
                accountName = newAccount;
                changed = true;
            }
        }

        if (!changed) {
            return "Entendi que você quer editar a operação, mas não consegui identificar alterações válidas.";
        }

        ParsedTelegramMessage updated = new ParsedTelegramMessage(
                pending.intentType(),
                amount,
                description,
                date,
                pending.originalMessage(),
                categoryName,
                accountName,
                pending.startDate(),
                pending.endDate()
        );

        telegramPendingConfirmationService.savePending(telegramId, updated);

        return buildUpdatedPendingMessage(telegramId, updated);
    }

    private String buildUpdatedPendingMessage(Long telegramId, ParsedTelegramMessage parsedMessage) {
        String type = parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE ? "despesa" : "receita";

        String conta = resolvePreviewAccountName(parsedMessage, telegramId);

        String categoria = parsedMessage.categoryName() != null
                ? parsedMessage.categoryName()
                : "automática";

        return """
                ✅ Operação atualizada.
                
                Entendi esta %s:
                
                Valor: %s
                Descrição: %s
                Data: %s
                Conta: %s
                Categoria: %s
                
                Deseja confirmar e salvar?
                """.formatted(
                type,
                formatCurrency(parsedMessage.amount()),
                parsedMessage.description() != null ? parsedMessage.description() : "Não informada",
                formatDate(parsedMessage.date()),
                conta,
                categoria
        );
    }

    private String mapDefaultBotErrors(RestClientResponseException e) {
        return switch (e.getStatusCode().value()) {
            case 400 -> "A solicitação está inválida.";
            case 404 -> "Não encontrei uma conta vinculada a este Telegram. Use /connect ou /conectar CODIGO.";
            case 401, 403 -> "O bot não tem permissão para acessar esse recurso agora.";
            case 500 -> "Ocorreu um erro interno ao processar sua solicitação.";
            default -> "Ocorreu um erro ao processar sua solicitação.";
        };
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

    private boolean isConfirmationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("sim")
                || lower.equals("confirmar")
                || lower.equals("confirmado")
                || lower.equals("ok");
    }

    private boolean isCancellationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("cancelar")
                || lower.equals("cancelado")
                || lower.equals("cancelar operação")
                || lower.equals("cancelar operacao")
                || lower.equals("não")
                || lower.equals("nao");
    }

    private boolean looksLikeEditMessage(String messageText) {
        String lower = normalizeText(messageText);

        return lower.contains("muda valor")
                || lower.contains("muda o valor")
                || lower.contains("mude valor")
                || lower.contains("mude o valor")
                || lower.contains("altera valor")
                || lower.contains("altera o valor")
                || lower.contains("altere valor")
                || lower.contains("altere o valor")
                || lower.contains("corrige valor")
                || lower.contains("corrige o valor")
                || lower.contains("corrija valor")
                || lower.contains("corrija o valor")
                || lower.contains("troca valor")
                || lower.contains("troque valor")
                || lower.contains("muda descricao")
                || lower.contains("muda a descricao")
                || lower.contains("altera descricao")
                || lower.contains("altera a descricao")
                || lower.contains("corrige descricao")
                || lower.contains("corrige a descricao")
                || lower.contains("muda categoria")
                || lower.contains("muda a categoria")
                || lower.contains("troca categoria")
                || lower.contains("troca a categoria")
                || lower.contains("altera categoria")
                || lower.contains("altera a categoria")
                || lower.contains("muda data")
                || lower.contains("muda a data")
                || lower.contains("troca data")
                || lower.contains("troca a data")
                || lower.contains("altera data")
                || lower.contains("altera a data")
                || lower.contains("muda conta")
                || lower.contains("muda a conta")
                || lower.contains("troca conta")
                || lower.contains("troca a conta")
                || lower.contains("altera conta")
                || lower.contains("altera a conta")
                || lower.contains("usa a conta")
                || lower.contains("coloca na conta")
                || lower.contains("coloca a conta")
                || lower.startsWith("data de ")
                || lower.startsWith("data para ")
                || lower.startsWith("data pra ")
                || lower.contains("pra ")
                || lower.contains("para ");
    }

    private String normalizeText(String text) {
        if(text == null){
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private boolean startsWithCommand(String messageText, String... commands) {
        for (String command : commands) {
            if (messageText.startsWith(command)) {
                return true;
            }
        }
        return false;
    }

    private String defaultText(String value) {
        return value != null && !value.isBlank() ? value : "Não informado";
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

    private String mapIntentToTransactionType(TelegramIntentType intentType) {
        return switch (intentType) {
            case CREATE_EXPENSE -> "EXPENSE";
            case CREATE_INCOME -> "INCOME";
            default -> throw new IllegalArgumentException("Intento inválido para criação de transação.");
        };
    }

    private BigDecimal extractAmountFromEdit(String text) {
        try {
            String normalized = text.replace("R$", "").trim();
            String extracted = normalized.replaceAll(".*?(\\d+[\\.,]?\\d{0,2}).*", "$1");
            return parseBrazilianNumber(extracted);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDescriptionFromEdit(String text) {
        String cleaned = normalizeText(text)
                .replaceFirst(".*?descricao\\s+para\\s+", "")
                .replaceFirst(".*?descricao\\s+", "")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String extractCategoryFromEdit(String text) {
        String cleaned = normalizeText(text)
                .replaceFirst(".*?categoria\\s+para\\s+", "")
                .replaceFirst(".*?categoria\\s+", "")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        return capitalizeWords(cleaned);
    }

    private LocalDate extractDateFromEdit(String text) {
        String lower = normalizeText(text);

        if (lower.contains("hoje")) {
            return LocalDate.now();
        }

        if (lower.contains("ontem")) {
            return LocalDate.now().minusDays(1);
        }

        if (lower.contains("amanha")) {
            return LocalDate.now().plusDays(1);
        }

        Matcher slashMatcher = EDIT_DATE_SLASH_PATTERN.matcher(lower);
        if (slashMatcher.find()) {
            try {
                return LocalDate.parse(slashMatcher.group(1), FLEXIBLE_SLASH_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        Matcher dashMatcher = EDIT_DATE_DASH_PATTERN.matcher(lower);
        if (dashMatcher.find()) {
            try {
                return LocalDate.parse(dashMatcher.group(1), FLEXIBLE_DASH_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        Matcher dayOnlyMatcher = EDIT_DAY_ONLY_PATTERN.matcher(lower);
        if (dayOnlyMatcher.find()) {
            try {
                int day = Integer.parseInt(dayOnlyMatcher.group(1));
                YearMonth currentMonth = YearMonth.now();

                if (day >= 1 && day <= currentMonth.lengthOfMonth()) {
                    return currentMonth.atDay(day);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String capitalizeWords(String text) {
        String[] parts = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(" ");
            }

            result.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase());
        }

        return result.toString();
    }

    private static final Pattern EDIT_DATE_SLASH_PATTERN = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
    private static final Pattern EDIT_DATE_DASH_PATTERN = Pattern.compile("(\\d{1,2}-\\d{1,2}-\\d{4})");
    private static final Pattern EDIT_DAY_ONLY_PATTERN = Pattern.compile("\\bdia\\s+(\\d{1,2})\\b");

    private static final DateTimeFormatter FLEXIBLE_SLASH_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    private static final DateTimeFormatter FLEXIBLE_DASH_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('-')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter();

    private String extractAccountFromEdit(String text) {
        String cleaned = normalizeText(text)
                .replaceFirst(".*?conta\\s+para\\s+", "")
                .replaceFirst(".*?conta\\s+pra\\s+", "")
                .replaceFirst(".*?usa\\s+a\\s+conta\\s+", "")
                .replaceFirst(".*?coloca\\s+na\\s+conta\\s+", "")
                .replaceFirst(".*?coloca\\s+a\\s+conta\\s+", "")
                .replaceFirst(".*?troca\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?troca\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?muda\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?muda\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?altera\\s+a\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?altera\\s+conta\\s+para\\s+", "")
                .replaceFirst(".*?conta\\s+", "")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        return capitalizeWords(cleaned);
    }

    private String resolvePreviewAccountName(ParsedTelegramMessage parsedMessage, Long telegramId) {
        if (parsedMessage.accountName() != null && !parsedMessage.accountName().isBlank()) {
            return parsedMessage.accountName();
        }

        try {
            TelegramDefaultAccountResponse response = financeBotApiClient.getDefaultAccount(telegramId);

            if (response != null && response.accountName() != null && !response.accountName().isBlank()) {
                return response.accountName();
            }
        } catch (RestClientResponseException e) {
            return "conta padrão";
        } catch (Exception e) {
            return "conta padrão";
        }

        return "conta padrão";
    }

    private boolean containsAmountEditHint(String lower) {
        return lower.contains("valor");
    }

    private boolean containsDescriptionEditHint(String lower) {
        return lower.contains("descricao");
    }

    private boolean containsCategoryEditHint(String lower) {
        return lower.contains("categoria");
    }

    private boolean containsDateEditHint(String lower) {
        return lower.contains("data")
                || lower.contains("hoje")
                || lower.contains("ontem")
                || lower.contains("amanha")
                || EDIT_DATE_SLASH_PATTERN.matcher(lower).find()
                || EDIT_DATE_DASH_PATTERN.matcher(lower).find()
                || EDIT_DAY_ONLY_PATTERN.matcher(lower).find();
    }

    private boolean containsAccountEditHint(String lower) {
        return lower.contains("conta");
    }
}