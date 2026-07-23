package ru.kirzhq.wazzup.dto;

import java.util.List;

public record WazzupContactsResponse(
        Long count,
        List<WazzupContact> data
) {
}