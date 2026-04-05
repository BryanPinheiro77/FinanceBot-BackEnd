package com.financebot.telegrambot.service;

import com.financebot.telegrambot.client.FinanceBotApiClient;
import com.financebot.telegrambot.dto.TelegramLinkConfirmRequest;
import com.financebot.telegrambot.dto.TelegramLinkConfirmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

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
                """;
    }

    private String handleStart(String telegramFirstName, String telegramUsername) {
        String name = resolveDisplayName(telegramFirstName, telegramUsername);

        return """
                Bem-vindo%s ao Your Finance Assistant!
                                
                Eu posso ajudar você a conectar sua conta e, em breve, acompanhar suas finanças direto pelo Telegram.
                                
                Comandos disponíveis:
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                """.formatted(name != null ? ", " + name : "");
    }

    private String handleHelp() {
        return """
                Comandos disponíveis:
                                
                /start - Iniciar o bot
                /help - Ver ajuda
                /connect CODIGO - Conectar sua conta
                                
                Exemplo de conexão:
                /connect FIN-ABC123
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
}