package ru.kirzhq.wazzup.dto;

import java.time.Instant;

public record PendingContactResponse(
        String id,
        String chatType,
        String chatId,
        String name,
        String username,
        String phone,
        String source,
        Instant updatedAt,
        Instant lastActivityAt
) {}
