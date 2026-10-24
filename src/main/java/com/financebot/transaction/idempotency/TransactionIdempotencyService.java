package com.financebot.transaction.idempotency;

import com.financebot.security.crypto.EncryptionKeyHolder;
import com.financebot.security.crypto.FieldEncryptionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Service
public class TransactionIdempotencyService {
    private final TransactionIdempotencyStore store;
    private final IdempotencyFingerprint fingerprint;
    private final ObjectMapper json;
    private final TransactionIdempotencyProperties properties;
    private final TransactionTemplate transaction;

    public TransactionIdempotencyService(TransactionIdempotencyStore store, IdempotencyFingerprint fingerprint,
            ObjectMapper json, TransactionIdempotencyProperties properties, PlatformTransactionManager manager) {
        this.store = store;
        this.fingerprint = fingerprint;
        this.json = json;
        this.properties = properties;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }

    public <T> T execute(Long userId, Operation operation, String key, Object request,
                         Class<T> responseType, Supplier<T> action) {
        if (key == null) return action.get();
        String keyHash = fingerprint.keyHash(key);
        String requestHash = fingerprint.requestHash(request);
        byte[] context = (userId + ":" + operation + ":" + keyHash + ":" + requestHash)
                .getBytes(StandardCharsets.UTF_8);
        try {
            return transaction.execute(status -> {
                if (!store.claim(userId, operation.name(), keyHash, requestHash, properties.getTtlSeconds())) {
                    var stored = store.find(userId, operation.name(), keyHash);
                    if (!requestHash.equals(stored.requestHash())) throw new IdempotencyConflictException();
                    if (stored.encryptedResponse() == null) {
                        throw new IdempotencyUnavailableException(new IllegalStateException("Resposta idempotente ausente"));
                    }
                    return json.readValue(EncryptionKeyHolder.get().decrypt(stored.encryptedResponse(), context), responseType);
                }
                T response = action.get();
                String encrypted = EncryptionKeyHolder.get().encrypt(json.writeValueAsString(response), context);
                store.complete(userId, operation.name(), keyHash, encrypted);
                return response;
            });
        } catch (TransientDataAccessException | TransactionTimedOutException | FieldEncryptionException | JacksonException exception) {
            throw new IdempotencyUnavailableException(exception);
        }
    }

    public int purgeExpired() {
        return store.deleteExpired(properties.getCleanupBatchSize());
    }

    public enum Operation { CREATE_TRANSACTION, CREATE_INSTALLMENT, CREATE_EXISTING_INSTALLMENT }
}
