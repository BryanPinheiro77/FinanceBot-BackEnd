package com.financebot.telegrambot.parser;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Classifica o fluxo de uma mensagem sem construir o resultado parseado.
 * As extrações necessárias são fornecidas pela camada de regras de texto.
 */
public class TelegramIntentClassifier {

    private final Function<String, Integer> installmentCountExtractor;
    private final Function<String, BigDecimal> installmentPurchaseAmountExtractor;
    private final Function<String, Integer> firstRemainingInstallmentExtractor;

    public TelegramIntentClassifier(
            Function<String, Integer> installmentCountExtractor,
            Function<String, BigDecimal> installmentPurchaseAmountExtractor,
            Function<String, Integer> firstRemainingInstallmentExtractor
    ) {
        this.installmentCountExtractor = installmentCountExtractor;
        this.installmentPurchaseAmountExtractor = installmentPurchaseAmountExtractor;
        this.firstRemainingInstallmentExtractor = firstRemainingInstallmentExtractor;
    }

    public boolean isInstallmentPurchaseCapacityQuery(String text) {
        boolean asksCapacity = text.contains("consigo")
                || text.contains("cabe no meu orcamento")
                || text.contains("cabe no orcamento")
                || text.contains("se eu parcelar");

        return asksCapacity
                && (text.contains("compra") || text.contains("parcel"))
                && installmentCountExtractor.apply(text) != null
                && installmentPurchaseAmountExtractor.apply(text) != null;
    }

    public boolean isTransactionTotalQuery(String text) {
        return text.contains("quanto gastei")
                || text.contains("quanto recebi")
                || text.contains("quanto entrou")
                || text.contains("total gasto")
                || text.contains("total recebido")
                || text.contains("gastei quanto")
                || text.contains("recebi quanto")
                || text.contains("entrou quanto");
    }

    public boolean isMonthAnalysisQuery(String text) {
        return text.contains("analise") || text.contains("resumo financeiro");
    }

    public boolean looksLikeExpense(String text) {
        return text.contains("gastei")
                || text.contains("paguei")
                || text.contains("comprei")
                || text.contains("despesa")
                || text.contains("pago")
                || text.contains("compra")
                || text.contains("boleto")
                || text.contains("debito")
                || text.contains("debitei")
                || text.contains("saiu da conta")
                || text.contains("saiu do banco");
    }

    public boolean looksLikeInstallmentExpense(String text) {
        Integer installmentCount = installmentCountExtractor.apply(text);
        return looksLikeExpense(text) && installmentCount != null && installmentCount >= 2;
    }

    public boolean looksLikeExistingInstallmentExpense(String text) {
        Integer installmentCount = installmentCountExtractor.apply(text);
        Integer firstRemainingInstallmentNumber = firstRemainingInstallmentExtractor.apply(text);

        return looksLikeInstallmentSubject(text)
                && installmentCount != null
                && installmentCount >= 2
                && firstRemainingInstallmentNumber != null
                && firstRemainingInstallmentNumber <= installmentCount;
    }

    private boolean looksLikeInstallmentSubject(String text) {
        return looksLikeExpense(text)
                || text.contains("parcelamento")
                || text.contains("financiamento")
                || text.contains("tenho");
    }

    public boolean looksLikeIncome(String text) {
        return text.contains("recebi")
                || text.contains("ganhei")
                || text.contains("entrou")
                || text.contains("entrada")
                || text.contains("caiu")
                || text.contains("depositaram")
                || text.contains("deposito")
                || text.contains("pix recebido")
                || text.contains("me pagaram");
    }

    public boolean isInstallmentCountQuery(String text) {
        return (text.contains("quantas parcelas") || text.contains("quantos parcelamentos"))
                && (text.contains("tenho")
                || text.contains("nesse mes")
                || text.contains("neste mes")
                || text.contains("esse mes")
                || text.contains("este mes")
                || text.contains("mes passado")
                || text.contains("hoje")
                || text.contains("ontem"));
    }

    public boolean isActiveInstallmentsQuery(String text) {
        return text.contains("parcelamentos ativos")
                || text.contains("parcelamento ativo")
                || text.contains("parcelas ativas")
                || text.contains("parcela ativa")
                || text.contains("tenho parcelamentos ativos");
    }

    public boolean isInstallmentRemainingQuery(String text) {
        return text.contains("quantas parcelas faltam")
                || text.contains("quantas faltam")
                || text.contains("faltam quantas parcelas")
                || text.contains("quantas parcelas restam")
                || text.contains("restam quantas parcelas");
    }

    public boolean isInstallmentEndDateQuery(String text) {
        return text.contains("quando acaba o parcelamento")
                || text.contains("quando termina o parcelamento")
                || text.contains("quando acaba minha parcela")
                || text.contains("quando acaba meu parcelamento")
                || text.contains("quando termina minha parcela")
                || text.contains("quando termina meu parcelamento")
                || text.contains("quando termina a parcela")
                || text.contains("quando acaba a parcela")
                || text.contains("quando termina parcela")
                || text.contains("quando acaba parcela");
    }
}
