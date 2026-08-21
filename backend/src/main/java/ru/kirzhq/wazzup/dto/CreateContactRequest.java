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
        @Size(max = 40, message = "Номер телефона слишком длинный")
        String phone,

        @Size(max = 20, message = "Тип мессенджера слишком длинный")
        String chatType,

        @NotBlank(message = "Без первого сообщения создать контакт невозможно")
        @Size(max = 4000, message = "Сообщение не должно превышать 4000 символов")
        String message
) {
}
