package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;

public record ApiKeyRequest(
        @NotBlank(message = "API-ключ не может быть пустым")
        String apiKey
) {
}