package ru.kirzhq.wazzup.service;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;

import java.util.List;
import java.util.Map;

@Service
public class PartnerWebhookService {
    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookService.class);
    private final PartnerOauthService oauthService;
    private final WazzupPartnerProperties properties;
    private final ContactService contactService;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://tech.wazzup24.com/v2")
            .build();

    public PartnerWebhookService(
            PartnerOauthService oauthService,
            WazzupPartnerProperties properties,
            ContactService contactService
    ) {
        this.oauthService = oauthService;
        this.properties = properties;
        this.contactService = contactService;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 21_600_000)
    public void ensureSubscriptions() {
        if (!properties.hasWebhookUrl() || !oauthService.getStatus().connected()) return;
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/webhooks")
                    .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                    .retrieve().body(Map.class);
            List<?> subscriptions = response != null && response.get("data") instanceof List<?> list
                    ? list : List.of();
            ensureSubscription(subscriptions, "message.add");
            ensureSubscription(subscriptions, "messages_dump.status_update");
        } catch (Exception exception) {
            log.warn("Не удалось настроить вебхуки Wazzup: {}", exception.getMessage());
        }
    }

    private void ensureSubscription(List<?> subscriptions, String event) {
        boolean exists = subscriptions.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> event.equals(item.get("event"))
                        && properties.webhookUrl().equals(item.get("url")));
        if (exists) return;

        restClient.post().uri("/webhooks")
                .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("data", List.of(Map.of(
                        "url", properties.webhookUrl(),
                        "event", event
                ))))
                .retrieve().toBodilessEntity();
        log.info("Создана подписка Wazzup: {} -> {}", event, properties.webhookUrl());
    }

    public void accept(Map<String, Object> payload) {
        if (!"message.add".equals(payload.get("event"))) return;
        Object data = payload.get("data");
        if (data instanceof List<?> list) {
            list.stream().filter(Map.class::isInstance)
                    .map(Map.class::cast).forEach(this::acceptMessage);
        } else if (data instanceof Map<?, ?> message) {
            acceptMessage(message);
        }
    }

    private void acceptMessage(Map<?, ?> message) {
        Object recipientValue = message.get("recipient");
        if (!(recipientValue instanceof Map<?, ?> recipient)) return;
        if (!"max".equalsIgnoreCase(string(recipient.get("chat_type")))) return;
        contactService.ensureChatContact(
                string(recipient.get("chat_type")),
                string(recipient.get("chat_id")),
                string(recipient.get("username")),
                string(recipient.get("phone")),
                string(recipient.get("name"))
        );
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
