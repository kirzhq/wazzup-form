package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContactRequest(

        @NotBlank(message = "Имя контакта обязательно")
        @Size(
                max = 200,
                message = "Имя контакта не должно превышать 200 символов"
        )
        String name,

        @Size(max = 40, message = "Номер телефона слишком длинный")
        String phone,

        @NotBlank(message = "Социальная сеть обязательна")
        @Size(max = 20, message = "Тип мессенджера слишком длинный")
        String chatType,

        @Size(max = 200, message = "Идентификатор чата слишком длинный")
        String chatId
) {
}
