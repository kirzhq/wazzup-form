package ru.kirzhq.wazzup.dto;

public record PartnerStatusResponse(
        boolean configured,
        boolean connected
) {
}
