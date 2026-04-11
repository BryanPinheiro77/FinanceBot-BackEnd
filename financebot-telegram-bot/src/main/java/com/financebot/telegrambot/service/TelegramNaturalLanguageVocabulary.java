package com.financebot.telegrambot.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class TelegramNaturalLanguageVocabulary {

    private static final Map<String, String> CATEGORY_ALIASES = createCategoryAliases();
    private static final Map<String, String> ACCOUNT_ALIASES = createAccountAliases();

    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String findCategoryName(String text) {
        return findAliasValue(text, CATEGORY_ALIASES);
    }

    public String resolveAccountName(String text) {
        return findAliasValue(text, ACCOUNT_ALIASES);
    }

    public String stripLeadingDescriptionNoise(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return text
                .replaceFirst("^(?:um|uma|o|a|os|as|meu|minha|meus|minhas)\\s+", "")
                .replaceFirst("^(?:no|na|nos|nas|num|numa)\\s+", "")
                .replaceFirst("^(?:de|do|da|dos|das)\\s+", "")
                .trim();
    }

    public String stripLeadingQueryTargetNoise(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return text
                .replaceFirst("^(?:e)\\s+", "")
                .replaceFirst("^(?:do|da|de|dos|das|o|a|os|as|meu|minha|meus|minhas)\\s+", "")
                .trim();
    }

    private String findAliasValue(String text, Map<String, String> aliases) {
        String normalized = normalize(text);

        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (containsAlias(normalized, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private boolean containsAlias(String normalizedText, String alias) {
        Pattern pattern = Pattern.compile("(^|\\b)" + Pattern.quote(alias) + "(\\b|$)");
        return pattern.matcher(normalizedText).find();
    }

    private static Map<String, String> createCategoryAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("supermercado", "Mercado");
        aliases.put("mercado", "Mercado");
        aliases.put("atacadao", "Mercado");
        aliases.put("atacado", "Mercado");
        aliases.put("mcdonalds", "Alimentação");
        aliases.put("mc donalds", "Alimentação");
        aliases.put("restaurante", "Alimentação");
        aliases.put("lanchonete", "Alimentação");
        aliases.put("alimentacao", "Alimentação");
        aliases.put("ifood", "Alimentação");
        aliases.put("lanche", "Alimentação");
        aliases.put("pizza", "Alimentação");
        aliases.put("comida", "Alimentação");
        aliases.put("combustivel", "Combustível");
        aliases.put("gasolina", "Combustível");
        aliases.put("shell", "Combustível");
        aliases.put("posto", "Combustível");
        aliases.put("etanol", "Combustível");
        aliases.put("diesel", "Combustível");
        aliases.put("transporte", "Transporte");
        aliases.put("uber", "Transporte");
        aliases.put("99", "Transporte");
        aliases.put("taxi", "Transporte");
        aliases.put("onibus", "Transporte");
        aliases.put("metro", "Transporte");
        aliases.put("farmacia", "Saúde");
        aliases.put("saude", "Saúde");
        aliases.put("remedio", "Saúde");
        aliases.put("medico", "Saúde");
        aliases.put("consulta", "Saúde");
        aliases.put("moradia", "Moradia");
        aliases.put("aluguel", "Moradia");
        aliases.put("condominio", "Moradia");
        aliases.put("salario", "Salário");
        aliases.put("freela", "Freelance");
        aliases.put("freelance", "Freelance");
        aliases.put("outros", "Outros");
        return aliases;
    }

    private static Map<String, String> createAccountAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("mercado pago", "Mercado Pago");
        aliases.put("banco do brasil", "Banco do Brasil");
        aliases.put("nubank", "Nubank");
        aliases.put("inter", "Inter");
        aliases.put("picpay", "PicPay");
        aliases.put("itau", "Itaú");
        aliases.put("bradesco", "Bradesco");
        aliases.put("caixa", "Caixa");
        aliases.put("santander", "Santander");
        aliases.put("bb", "Banco do Brasil");
        return aliases;
    }
}
