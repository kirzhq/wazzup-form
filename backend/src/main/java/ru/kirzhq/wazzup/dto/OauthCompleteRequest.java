package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;

public record OauthCompleteRequest(
        @NotBlank String code,
        @NotBlank String state
) {
}
