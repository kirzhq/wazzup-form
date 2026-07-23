package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContactRequest(

        @NotBlank(message = "Имя контакта обязательно")
        @Size(
                max = 200,
                message = "Имя контакта не должно превышать 200 символов"
        )
        String name,

        @NotBlank(message = "Номер телефона обязателен")
        String phone,

        @NotBlank(message = "Не указан ответственный сотрудник")
        String responsibleUserId,

        String chatType
) {
}