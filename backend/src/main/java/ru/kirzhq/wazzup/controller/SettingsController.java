package ru.kirzhq.wazzup.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.kirzhq.wazzup.dto.ApiKeyRequest;
import ru.kirzhq.wazzup.service.SettingsService;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Map<String, Boolean> getSettingsStatus() {
        return Map.of(
                "configured",
                settingsService.isConfigured()
        );
    }

    @PutMapping("/api-key")
    public Map<String, String> saveApiKey(
            @Valid @RequestBody ApiKeyRequest request
    ) {
        settingsService.saveApiKey(request.apiKey());

        return Map.of(
                "message",
                "API-ключ сохранён"
        );
    }
}