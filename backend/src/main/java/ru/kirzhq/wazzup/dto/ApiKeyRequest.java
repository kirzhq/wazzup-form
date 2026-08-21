package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyRequest(
        @NotBlank(message = "API-ключ не может быть пустым")
        @Size(max = 512, message = "API-ключ слишком длинный")
        String apiKey
) {
}
