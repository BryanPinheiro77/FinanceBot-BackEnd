package com.financebot.telegrambot.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramCommandMatcher {

    private final TelegramTextNormalizer telegramTextNormalizer;

    public boolean startsWithCommand(String messageText, String... commands) {
        for (String command : commands) {
            if (messageText.startsWith(command)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsGreeting(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("oi")
                || lower.equals("olá")
                || lower.equals("ola")
                || lower.equals("bom dia")
                || lower.equals("boa tarde")
                || lower.equals("boa noite");
    }

    public boolean looksLikeConnectionIntent(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.contains("conectar")
                || lower.contains("vincular")
                || lower.contains("ligar conta")
                || lower.contains("linkar")
                || lower.contains("telegram");
    }

    public boolean isConfirmationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("sim")
                || lower.equals("confirmar")
                || lower.equals("confirmado")
                || lower.equals("ok");
    }

    public boolean isCancellationMessage(String messageText) {
        String lower = messageText.toLowerCase();

        return lower.equals("cancelar")
                || lower.equals("cancelado")
                || lower.equals("cancelar operação")
                || lower.equals("cancelar operacao")
                || lower.equals("não")
                || lower.equals("nao");
    }

    public boolean looksLikeEditMessage(String messageText) {
        String lower = telegramTextNormalizer.normalize(messageText);

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
                || lower.contains("muda o dia")
                || lower.contains("muda dia")
                || lower.contains("muda pro dia")
                || lower.contains("muda para o dia")
                || lower.contains("muda para dia")
                || lower.contains("mudar o dia")
                || lower.contains("mudar dia")
                || lower.contains("mudar pro dia")
                || lower.contains("mudar para o dia")
                || lower.contains("mudar para dia")
                || lower.contains("troca data")
                || lower.contains("troca a data")
                || lower.contains("troca o dia")
                || lower.contains("troca dia")
                || lower.contains("troca pro dia")
                || lower.contains("troca para o dia")
                || lower.contains("troca para dia")
                || lower.contains("trocar o dia")
                || lower.contains("trocar dia")
                || lower.contains("trocar pro dia")
                || lower.contains("trocar para o dia")
                || lower.contains("trocar para dia")
                || lower.contains("altera data")
                || lower.contains("altera a data")
                || lower.contains("altera o dia")
                || lower.contains("altera dia")
                || lower.contains("altera pro dia")
                || lower.contains("altera para o dia")
                || lower.contains("altera para dia")
                || lower.contains("alterar o dia")
                || lower.contains("alterar dia")
                || lower.contains("alterar pro dia")
                || lower.contains("alterar para o dia")
                || lower.contains("alterar para dia")
                || lower.contains("corrige o dia")
                || lower.contains("corrige dia")
                || lower.contains("corrige pro dia")
                || lower.contains("corrige para o dia")
                || lower.contains("corrige para dia")
                || lower.contains("corrigir o dia")
                || lower.contains("corrigir dia")
                || lower.contains("corrigir pro dia")
                || lower.contains("corrigir para o dia")
                || lower.contains("corrigir para dia")
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
}
