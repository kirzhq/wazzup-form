package ru.kirzhq.wazzup.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.repository.AppSettingsRepository;

import java.time.Instant;

@Service
public class SettingsService {

    private final AppSettingsRepository repository;

    public SettingsService(AppSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveApiKey(String apiKey) {
        String normalizedApiKey = apiKey.trim();

        AppSettings settings = repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> new AppSettings(apiKey));

        settings.setApiKey(normalizedApiKey);
        repository.save(settings);
    }

    public boolean isConfigured() {
        return repository.count() > 0;
    }

    public String getApiKey() {
        return getSettings().getApiKey();
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
