package com.financebot.telegrambot.intent;

public enum TelegramIntentType {
    UNKNOWN,
    CREATE_EXPENSE,
    CREATE_INCOME,
    QUERY_MONTH_EXPENSE_TOTAL,
    QUERY_MONTH_INCOME_TOTAL,
    QUERY_MONTH_ANALYSIS,
    QUERY_TRANSACTION_TOTAL
}