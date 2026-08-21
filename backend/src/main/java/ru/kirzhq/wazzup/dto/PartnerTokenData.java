package ru.kirzhq.wazzup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerTokenData(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") long expiresIn
) {
}
