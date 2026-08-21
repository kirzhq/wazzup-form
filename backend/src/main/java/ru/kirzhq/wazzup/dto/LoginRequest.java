package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Введите номер телефона")
        @Size(max = 40, message = "Номер телефона слишком длинный")
        String phone
) {
}
