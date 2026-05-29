package com.financebot.transaction.application.port.out;

import com.financebot.transaction.domain.Transaction;

import java.util.List;

public interface SaveTransactionPort {

    Transaction save(Transaction transaction);

    List<Transaction> saveAll(List<Transaction> transactions);
}