package ru.kirzhq.wazzup.dto;

public record WazzupContactData(
        String chatType,
        String chatId,
        String username,
        String phone
) {
}