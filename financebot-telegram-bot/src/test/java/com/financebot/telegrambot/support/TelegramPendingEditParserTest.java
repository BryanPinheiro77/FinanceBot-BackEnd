package com.financebot.telegrambot.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramPendingEditParserTest {

    private TelegramPendingEditParser telegramPendingEditParser;

    @BeforeEach
    void setUp() {
        telegramPendingEditParser = new TelegramPendingEditParser(new TelegramTextNormalizer());
    }

    @Test
    @DisplayName("deve extrair novo valor da edicao")
    void shouldExtractAmountEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda o valor para R$ 80,50");

        assertThat(result.changed()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("80.50");
    }

    @Test
    @DisplayName("deve extrair nova descricao da edicao")
    void shouldExtractDescriptionEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda a descricao para mercado extra");

        assertThat(result.changed()).isTrue();
        assertThat(result.description()).isEqualTo("mercado extra");
    }

    @Test
    @DisplayName("deve extrair nova categoria da edicao")
    void shouldExtractCategoryEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("altera categoria para supermercado");

        assertThat(result.changed()).isTrue();
        assertThat(result.categoryName()).isEqualTo("Supermercado");
    }

    @Test
    @DisplayName("deve extrair nova conta da edicao")
    void shouldExtractAccountEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("usa a conta nubank");

        assertThat(result.changed()).isTrue();
        assertThat(result.accountName()).isEqualTo("Nubank");
    }

    @Test
    @DisplayName("deve extrair nova data explicita da edicao")
    void shouldExtractExplicitDateEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda data para 05/06/2026");

        assertThat(result.changed()).isTrue();
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    @DisplayName("deve extrair nova data com dia e mes sem ano")
    void shouldExtractDateWithDayAndMonthWithoutYear() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("alterar pro dia 31 do 05");

        assertThat(result.changed()).isTrue();
        assertThat(result.date()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 5, 31));
    }

    @Test
    @DisplayName("deve extrair nova data com dia para e mes sem ano")
    void shouldExtractDateWithDayToAndMonthWithoutYear() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda o dia para 10 do 06");

        assertThat(result.changed()).isTrue();
        assertThat(result.date()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 6, 10));
    }

    @Test
    @DisplayName("deve extrair nova data com barra e sem ano")
    void shouldExtractSlashDateWithoutYear() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda para 31/05");

        assertThat(result.changed()).isTrue();
        assertThat(result.date()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 5, 31));
    }

    @Test
    @DisplayName("deve extrair multiplas alteracoes da mesma mensagem")
    void shouldExtractMultipleEditsFromSameMessage() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda valor para 80 e a categoria para mercado e a conta nubank");

        assertThat(result.changed()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("80");
        assertThat(result.categoryName()).isEqualTo("Mercado");
        assertThat(result.accountName()).isEqualTo("Nubank");
    }
}
