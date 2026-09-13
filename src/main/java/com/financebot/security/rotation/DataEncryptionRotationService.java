package com.financebot.security.rotation;

import com.financebot.security.crypto.EncryptionKeyHolder;
import com.financebot.security.crypto.FieldEncryptionException;
import com.financebot.security.crypto.RotatingFieldEncryption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DataEncryptionRotationService {

    private final DataEncryptionRotationRepository repository;

    DataEncryptionRotationService(DataEncryptionRotationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int rotateNextBatch(int batchSize) {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("O lote de rotação deve estar entre 1 e 1000");
        }

        RotatingFieldEncryption encryption = EncryptionKeyHolder.get();
        var candidates = repository.findNextBatch(encryption.activePrefix(), batchSize);

        for (EncryptedFieldRotationCandidate candidate : candidates) {
            rotateCandidate(candidate, encryption);
        }
        return candidates.size();
    }

    private void rotateCandidate(
            EncryptedFieldRotationCandidate candidate,
            RotatingFieldEncryption encryption
    ) {
        String plaintext;
        int updatedRows;
        if (candidate.encryptedValue() != null) {
            plaintext = encryption.decrypt(candidate.encryptedValue());
            updatedRows = repository.replaceEncryptedValue(
                    candidate.userId(),
                    candidate.encryptedValue(),
                    encryption.encrypt(plaintext)
            );
        } else if (candidate.legacyValue() != null) {
            plaintext = candidate.legacyValue().toPlainString();
            updatedRows = repository.migrateLegacyValue(candidate.userId(), encryption.encrypt(plaintext));
        } else {
            throw new FieldEncryptionException("Registro sem valor disponível para rotação");
        }

        if (updatedRows != 1) {
            throw new FieldEncryptionException("A rotação foi interrompida por alteração concorrente");
        }
    }
}
