package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Введите номер телефона")
        String phone
) {
}