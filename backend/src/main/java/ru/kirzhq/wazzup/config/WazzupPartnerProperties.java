package ru.kirzhq.wazzup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wazzup.partner")
public record WazzupPartnerProperties(
        String clientId,
        String email,
        String password,
        String redirectUri,
        String encryptionKey,
        String webhookUrl,
        boolean syncEnabled
) {
    public boolean isConfigured() {
        return hasText(clientId)
                && hasText(email)
                && hasText(password)
                && hasText(redirectUri)
                && hasText(encryptionKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public boolean hasWebhookUrl() {
        return hasText(webhookUrl);
    }
}
