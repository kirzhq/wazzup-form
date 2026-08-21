package ru.kirzhq.wazzup.dto;

public record SendMessageRequest(
        String channelId,
        String chatType,
        String chatId,
        String phone,
        String text,
        String crmUserId
) {
}
