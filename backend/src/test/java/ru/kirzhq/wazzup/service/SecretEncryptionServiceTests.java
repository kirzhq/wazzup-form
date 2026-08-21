package ru.kirzhq.wazzup.service;

import org.junit.jupiter.api.Test;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretEncryptionServiceTests {

    private final SecretEncryptionService service = new SecretEncryptionService(
            new WazzupPartnerProperties(
                    null, null, null, null,
                    "test-encryption-key", null, null, false
            )
    );

    @Test
    void encryptsAndDecryptsSecret() {
        String encrypted = service.encrypt("api-key-value");

        assertNotEquals("api-key-value", encrypted);
        assertEquals("api-key-value", service.decrypt(encrypted));
    }

    @Test
    void rejectsModifiedCiphertext() {
        String encrypted = service.encrypt("api-key-value");
        char replacement = encrypted.endsWith("A") ? 'B' : 'A';
        String modified = encrypted.substring(0, encrypted.length() - 1) + replacement;

        assertThrows(IllegalStateException.class, () -> service.decrypt(modified));
    }
}
