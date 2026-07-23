package ru.kirzhq.wazzup.dto;

public record LoginResponse(
        String id,
        String name,
        String phone,
        Long accountId
) {
}