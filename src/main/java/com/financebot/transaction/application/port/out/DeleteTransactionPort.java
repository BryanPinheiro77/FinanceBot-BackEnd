package com.financebot.transaction.application.port.out;

import com.financebot.transaction.domain.Transaction;

public interface DeleteTransactionPort {

    void delete(Transaction transaction);
}