package com.financebot.telegrambot.client;

import com.financebot.telegrambot.dto.request.CreateTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.CreateInstallmentTransactionFromTelegramRequest;
import com.financebot.telegrambot.dto.request.CreateExistingInstallmentTransactionFromTelegramRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class FinanceBotApiClientIdempotencyTest {
    @Test void allCreationEndpointsForwardStableKeyAndInternalAuthentication() throws Exception {
        var observed = new LinkedBlockingQueue<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/telegram/transactions", exchange -> {
            observed.add(exchange.getRequestURI().getPath() + ":"
                    + exchange.getRequestHeaders().getFirst("Idempotency-Key") + ":"
                    + exchange.getRequestHeaders().getFirst("X-Internal-Service-Token"));
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            var client = new FinanceBotApiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-internal");
            client.createTransaction(new CreateTransactionFromTelegramRequest(123L, "EXPENSE", null, null, null, null, null), "stable-key-123");
            client.createInstallmentTransaction(new CreateInstallmentTransactionFromTelegramRequest(123L, null, null, null, null, null, 3), "stable-key-123");
            client.createExistingInstallmentTransaction(new CreateExistingInstallmentTransactionFromTelegramRequest(123L, null, null, null, null, null, null, 5, 3), "stable-key-123");
            for (String suffix : new String[]{"", "/installments", "/installments/existing"}) {
                assertThat(observed.poll(5, TimeUnit.SECONDS))
                        .isEqualTo("/telegram/transactions" + suffix + ":stable-key-123:test-internal");
            }
        } finally { server.stop(0); }
    }
}
