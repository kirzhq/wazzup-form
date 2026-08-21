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

        String phone,

        @NotBlank(message = "Социальная сеть обязательна")
        String chatType,

        String chatId
) {
}
