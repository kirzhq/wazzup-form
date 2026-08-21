package ru.kirzhq.wazzup.service;

import org.springframework.http.MediaType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;
import ru.kirzhq.wazzup.client.WazzupRestClientFactory;
import ru.kirzhq.wazzup.entity.ProcessedWebhook;
import ru.kirzhq.wazzup.repository.ProcessedWebhookRepository;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PartnerWebhookService {
    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookService.class);
    private final PartnerOauthService oauthService;
    private final WazzupPartnerProperties properties;
    private final ContactService contactService;
    private final PendingContactService pendingContactService;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final TaskExecutor taskExecutor;
    private final RestClient restClient = WazzupRestClientFactory.create(
            "https://tech.wazzup24.com/v2"
    );

    public PartnerWebhookService(
            PartnerOauthService oauthService,
            WazzupPartnerProperties properties,
            ContactService contactService,
            PendingContactService pendingContactService,
            ProcessedWebhookRepository processedWebhookRepository,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor
    ) {
        this.oauthService = oauthService;
        this.properties = properties;
        this.contactService = contactService;
        this.pendingContactService = pendingContactService;
        this.processedWebhookRepository = processedWebhookRepository;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 21_600_000)
    public void ensureSubscriptions() {
        if (!properties.hasWebhookUrl()
                || !properties.hasWebhookSecret()
                || !oauthService.getStatus().connected()) return;
        try {
            String webhookUrl = securedWebhookUrl();
            Map<?, ?> response = restClient.get()
                    .uri("/webhooks")
                    .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                    .retrieve().body(Map.class);
            List<?> subscriptions = response != null && response.get("data") instanceof List<?> list
                    ? list : List.of();
            ensureSubscription(subscriptions, webhookUrl, "message.add");
            ensureSubscription(subscriptions, webhookUrl, "messages_dump.status_update");
        } catch (Exception exception) {
            log.warn("Не удалось настроить вебхуки Wazzup: {}", exception.getMessage());
        }
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 86_400_000)
    public void cleanupProcessedWebhooks() {
        processedWebhookRepository.deleteByProcessedAtBefore(Instant.now().minusSeconds(2_592_000));
    }

    private void ensureSubscription(
            List<?> subscriptions,
            String webhookUrl,
            String event
    ) {
        boolean exists = subscriptions.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> event.equals(item.get("event"))
                        && webhookUrl.equals(item.get("url")));
        if (exists) return;

        restClient.post().uri("/webhooks")
                .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("data", List.of(Map.of(
                        "url", webhookUrl,
                        "event", event
                ))))
                .retrieve().toBodilessEntity();
        log.info("Создана подписка Wazzup на событие {}", event);
    }

    public boolean isAuthorized(String suppliedToken) {
        if (!properties.hasWebhookSecret() || suppliedToken == null) return false;
        return MessageDigest.isEqual(
                properties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String securedWebhookUrl() {
        return UriComponentsBuilder.fromUriString(properties.webhookUrl())
                .replaceQueryParam("token", properties.webhookSecret())
                .build()
                .encode()
                .toUriString();
    }

    public void accept(Map<String, Object> payload) {
        if (!"message.add".equals(payload.get("event"))) return;
        String idempotencyKey = idempotencyKey(payload);
        try {
            processedWebhookRepository.saveAndFlush(new ProcessedWebhook(idempotencyKey, Instant.now()));
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Повторный вебхук Wazzup пропущен: {}", idempotencyKey);
            return;
        }
        taskExecutor.execute(() -> {
            try {
                process(payload);
            } catch (Exception exception) {
                processedWebhookRepository.deleteById(idempotencyKey);
                log.warn("Не удалось обработать вебхук Wazzup {}: {}",
                        idempotencyKey, exception.getMessage());
            }
        });
    }

    private void process(Map<String, Object> payload) {
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
        String chatType = string(recipient.get("chat_type"));
        if (!("max".equalsIgnoreCase(chatType)
                || "telegram".equalsIgnoreCase(chatType)
                || "whatsapp".equalsIgnoreCase(chatType))) return;
        String chatId = string(recipient.get("chat_id"));
        String name = string(recipient.get("name"));
        String username = string(recipient.get("username"));
        String phone = string(recipient.get("phone"));
        String displayName = hasText(name) ? name : username;
        if (hasText(displayName)) {
            contactService.ensureChatContact(chatType, chatId, username, phone, displayName);
            return;
        }
        pendingContactService.remember(
                chatType, chatId, null, username, phone, "MESSAGE_ADD"
        );
    }

    private String idempotencyKey(Map<String, Object> payload) {
        Object metaValue = payload.get("meta");
        if (metaValue instanceof Map<?, ?> meta) {
            String key = string(meta.get("idempotency_key"));
            if (hasText(key)) return key;
        }
        Object data = payload.get("data");
        if (data instanceof Map<?, ?> message) {
            String messageId = string(message.get("message_id"));
            if (hasText(messageId)) return "message.add:" + messageId;
        }
        return "missing:" + UUID.randomUUID();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
