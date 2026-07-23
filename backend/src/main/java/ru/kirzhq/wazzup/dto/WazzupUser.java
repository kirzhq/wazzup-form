package ru.kirzhq.wazzup.dto;

public record WazzupUser(
        Long accountId,
        String id,
        String name,
        String phone
) {
}