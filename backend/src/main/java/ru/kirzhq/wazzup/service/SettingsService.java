package ru.kirzhq.wazzup.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.repository.AppSettingsRepository;

import java.time.Instant;

@Service
public class SettingsService {
    private static final String ENCRYPTED_PREFIX = "enc:v1:";

    private final AppSettingsRepository repository;
    private final SecretEncryptionService encryptionService;

    public SettingsService(
            AppSettingsRepository repository,
            SecretEncryptionService encryptionService
    ) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public void saveApiKey(String apiKey) {
        String normalizedApiKey = apiKey.trim();
        String encryptedApiKey = ENCRYPTED_PREFIX
                + encryptionService.encrypt(normalizedApiKey);

        AppSettings settings = repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> new AppSettings(encryptedApiKey));

        settings.setApiKey(encryptedApiKey);
        repository.save(settings);
    }

    public boolean isConfigured() {
        return repository.findFirstByOrderByIdAsc()
                .map(AppSettings::getApiKey)
                .filter(value -> !value.isBlank())
                .isPresent();
    }

    @Transactional
    public String getApiKey() {
        AppSettings settings = getSettings();
        String storedApiKey = settings.getApiKey();
        if (storedApiKey.startsWith(ENCRYPTED_PREFIX)) {
            return encryptionService.decrypt(
                    storedApiKey.substring(ENCRYPTED_PREFIX.length())
            );
        }

        // Миграция существующих установок: старые версии сохраняли ключ открытым текстом.
        String encryptedApiKey = ENCRYPTED_PREFIX
                + encryptionService.encrypt(storedApiKey);
        settings.setApiKey(encryptedApiKey);
        repository.save(settings);
        return storedApiKey;
    }

    public AppSettings getSettings() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() ->
                        new IllegalStateException("API-ключ ещё не настроен")
                );
    }

    @Transactional
    public void saveOauthRequest(String state, String encryptedVerifier) {
        AppSettings settings = getSettings();
        settings.setOauthState(state);
        settings.setOauthCodeVerifier(encryptedVerifier);
        repository.save(settings);
    }

    @Transactional
    public void savePartnerTokens(
            String encryptedAccessToken,
            String encryptedRefreshToken,
            Instant expiresAt
    ) {
        AppSettings settings = getSettings();
        settings.setPartnerAccessToken(encryptedAccessToken);
        settings.setPartnerRefreshToken(encryptedRefreshToken);
        settings.setPartnerTokenExpiresAt(expiresAt);
        settings.setOauthState(null);
        settings.setOauthCodeVerifier(null);
        repository.save(settings);
    }

    @Transactional
    public void saveMessagesExport(String exportId, Instant startedAt) {
        AppSettings settings = getSettings();
        settings.setMessagesExportId(exportId);
        settings.setMessagesSyncStartedAt(startedAt);
        repository.save(settings);
    }

    @Transactional
    public void completeMessagesSync(Instant syncedAt) {
        AppSettings settings = getSettings();
        settings.setMessagesExportId(null);
        settings.setMessagesSyncStartedAt(null);
        settings.setMessagesLastSyncedAt(syncedAt);
        repository.save(settings);
    }

    @Transactional
    public void requestMessagesSync() {
        AppSettings settings = getSettings();
        settings.setMessagesExportId(null);
        settings.setMessagesSyncStartedAt(null);
        settings.setMessagesLastSyncedAt(null);
        repository.save(settings);
    }
}
