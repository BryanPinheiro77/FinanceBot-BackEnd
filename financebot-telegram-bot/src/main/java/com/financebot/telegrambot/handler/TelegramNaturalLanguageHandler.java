package com.financebot.telegrambot.handler;

import com.financebot.telegrambot.dto.ParsedTelegramMessage;
import com.financebot.telegrambot.intent.TelegramIntentType;
import com.financebot.telegrambot.service.TelegramIntentService;
import com.financebot.telegrambot.service.TelegramQueryContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramNaturalLanguageHandler {

    private final TelegramIntentService telegramIntentService;
    private final TelegramQueryContextService telegramQueryContextService;
    private final TelegramFinancialQueryHandler telegramFinancialQueryHandler;
    private final TelegramTransactionPreviewHandler telegramTransactionPreviewHandler;

    public String handle(
            String normalizedMessage,
            Long telegramId
    ) {
        ParsedTelegramMessage parsedMessage = telegramIntentService.parse(normalizedMessage);
        parsedMessage = telegramQueryContextService.applyQueryContext(
                telegramId,
                normalizedMessage,
                parsedMessage
        );

        if (parsedMessage.intentType() != null && parsedMessage.intentType().name().startsWith("QUERY_")) {
            return telegramFinancialQueryHandler.handleQuery(parsedMessage, telegramId);
        }

        if (parsedMessage.intentType() == TelegramIntentType.CREATE_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_INSTALLMENT_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_EXISTING_INSTALLMENT_EXPENSE
                || parsedMessage.intentType() == TelegramIntentType.CREATE_INCOME) {
            return telegramTransactionPreviewHandler.handlePreview(telegramId, parsedMessage);
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
}
