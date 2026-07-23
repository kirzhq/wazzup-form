package ru.kirzhq.wazzup.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.repository.AppSettingsRepository;

@Service
public class SettingsService {

    private final AppSettingsRepository repository;

    public SettingsService(AppSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveApiKey(String apiKey) {
        AppSettings settings = repository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> new AppSettings(apiKey));

        settings.setApiKey(apiKey);
        repository.save(settings);
    }

    public boolean isConfigured() {
        return repository.count() > 0;
    }

    public String getApiKey() {
        return repository.findAll()
                .stream()
                .findFirst()
                .map(AppSettings::getApiKey)
                .orElseThrow(() ->
                        new IllegalStateException("API-ключ ещё не настроен")
                );
    }
}