package ru.kirzhq.wazzup.dto;

import java.util.List;

public record WazzupContact(
        String id,
        String responsibleUserId,
        String name,
        List<WazzupContactData> contactData,
        String uri
) {
}