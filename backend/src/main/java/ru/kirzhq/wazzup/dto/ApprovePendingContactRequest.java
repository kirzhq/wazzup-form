package ru.kirzhq.wazzup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovePendingContactRequest(
        @NotBlank @Size(max = 200) String name
) {}
