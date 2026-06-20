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
        assertThat(result.amountKind()).isEqualTo(TelegramPendingEditParser.EditedAmountKind.UNSPECIFIED);
    }

    @Test
    @DisplayName("deve identificar edicao explicita de valor total")
    void shouldIdentifyTotalAmountEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda o valor total para 6200");

        assertThat(result.changed()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("6200");
        assertThat(result.amountKind()).isEqualTo(TelegramPendingEditParser.EditedAmountKind.TOTAL);
    }

    @Test
    @DisplayName("deve identificar edicao explicita de valor mensal")
    void shouldIdentifyMonthlyAmountEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda o valor mensal para 620");

        assertThat(result.changed()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("620");
        assertThat(result.amountKind()).isEqualTo(TelegramPendingEditParser.EditedAmountKind.MONTHLY);
    }

    @Test
    @DisplayName("deve tratar edicao direta de numero como valor ambiguo")
    void shouldTreatBareNumberEditAsUnspecifiedAmount() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda pra 6200");

        assertThat(result.changed()).isTrue();
        assertThat(result.amount()).isEqualByComparingTo("6200");
        assertThat(result.amountKind()).isEqualTo(TelegramPendingEditParser.EditedAmountKind.UNSPECIFIED);
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

    @Test
    @DisplayName("deve extrair primeira parcela restante a partir de parcelas pagas")
    void shouldExtractFirstRemainingInstallmentFromPaidInstallments() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("ja paguei 5 parcelas");

        assertThat(result.changed()).isTrue();
        assertThat(result.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve extrair primeira parcela restante a partir da parcela atual")
    void shouldExtractFirstRemainingInstallmentFromCurrentInstallment() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("estou pagando a 6 parcela");

        assertThat(result.changed()).isTrue();
        assertThat(result.firstRemainingInstallmentNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("deve extrair primeira parcela restante de edicao direta da parcela")
    void shouldExtractFirstRemainingInstallmentFromDirectInstallmentEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda para a parcela 8");

        assertThat(result.changed()).isTrue();
        assertThat(result.firstRemainingInstallmentNumber()).isEqualTo(8);
    }

    @Test
    @DisplayName("deve extrair primeira parcela restante ao editar parcela paga")
    void shouldExtractFirstRemainingInstallmentFromPaidInstallmentEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda a parcela paga para 1");

        assertThat(result.changed()).isTrue();
        assertThat(result.firstRemainingInstallmentNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve extrair primeira parcela restante ao editar parcela diretamente")
    void shouldExtractFirstRemainingInstallmentFromDirectInstallmentToEdit() {
        TelegramPendingEditParser.PendingEditResult result =
                telegramPendingEditParser.parse("muda a parcela para 1");

        assertThat(result.changed()).isTrue();
        assertThat(result.firstRemainingInstallmentNumber()).isEqualTo(1);
    }
}
