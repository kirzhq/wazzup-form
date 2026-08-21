package ru.kirzhq.wazzup.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.exception.WazzupApiException;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Service
public class PartnerMessagesService {
    private static final Logger log = LoggerFactory.getLogger(PartnerMessagesService.class);
    private static final Instant INITIAL_SYNC_START = Instant.parse("2020-01-01T00:00:00Z");
    private final PartnerOauthService oauthService;
    private final SettingsService settingsService;
    private final PendingContactService pendingContactService;
    private final ContactService contactService;
    private final WazzupPartnerProperties properties;
    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://tech.wazzup24.com/v2").build();

    public PartnerMessagesService(PartnerOauthService oauthService,
                                  SettingsService settingsService,
                                  PendingContactService pendingContactService,
                                  ContactService contactService,
                                  WazzupPartnerProperties properties) {
        this.oauthService = oauthService;
        this.settingsService = settingsService;
        this.pendingContactService = pendingContactService;
        this.contactService = contactService;
        this.properties = properties;
    }

    @Scheduled(initialDelay = 15_000, fixedDelay = 60_000)
    public void synchronize() {
        if (!properties.syncEnabled() || !oauthService.getStatus().connected()) return;
        try {
            AppSettings settings = settingsService.getSettings();
            if (settings.getMessagesExportId() != null) {
                checkDump(settings.getMessagesExportId());
                return;
            }
            Instant lastSync = settings.getMessagesLastSyncedAt();
            if (lastSync == null || Duration.between(lastSync, Instant.now()).toHours() >= 24) {
                startDump(lastSync == null ? INITIAL_SYNC_START : lastSync.minus(Duration.ofDays(2)));
            }
        } catch (Exception exception) {
            log.warn("Не удалось синхронизировать собеседников Wazzup: {}", exception.getMessage());
        }
    }

    private void startDump(Instant startAt) {
        Map<?, ?> response = restClient.post().uri("/messages/messages_dump")
                .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("start_at", startAt.toString(), "end_at", Instant.now().toString()))
                .retrieve().body(Map.class);
        Map<?, ?> data = response != null && response.get("data") instanceof Map<?, ?> map ? map : Map.of();
        Object exportId = data.get("export_id");
        if (exportId == null) throw new WazzupApiException("Wazzup не вернул ID выгрузки");
        settingsService.saveMessagesExport(exportId.toString(), Instant.now());
        log.info("Создана задача выгрузки сообщений Wazzup: {}", exportId);
    }

    private void checkDump(String exportId) throws Exception {
        Map<?, ?> response = restClient.get()
                .uri("/messages/messages_dump/{exportId}", exportId)
                .headers(headers -> headers.setBearerAuth(oauthService.getAccessToken()))
                .retrieve().body(Map.class);
        Map<?, ?> data = response != null && response.get("data") instanceof Map<?, ?> map ? map : Map.of();
        String status = String.valueOf(data.get("status"));
        if (!("done".equals(status) || "webhook_failed".equals(status))) return;
        Object url = data.get("url");
        if (url == null) throw new WazzupApiException("В готовой выгрузке отсутствует ссылка");
        int imported = importCsv(download(url.toString()));
        settingsService.completeMessagesSync(Instant.now());
        log.info("Выгрузка сообщений Wazzup обработана, кандидатов на проверку: {}", imported);
    }

    private byte[] download(String url) {
        byte[] bytes = RestClient.create().get().uri(url).retrieve().body(byte[].class);
        if (bytes == null) throw new WazzupApiException("Wazzup вернул пустую выгрузку");
        return bytes;
    }

    private int importCsv(byte[] bytes) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        Map<String, TimedCandidate> candidates = new LinkedHashMap<>();
        try (InputStreamReader reader = new InputStreamReader(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            var parser = format.parse(reader);
            log.info("Колонки выгрузки Wazzup: {}", parser.getHeaderNames());
            for (CSVRecord record : parser) {
                String direction = field(record, "direction");
                boolean inbound = "inbound".equalsIgnoreCase(direction);
                boolean outbound = "outbound".equalsIgnoreCase(direction);
                if (!inbound && !outbound) continue;
                String transport = field(record, "transport", "chat_type");
                String chatId = field(record, "chat_id");
                if (transport == null || chatId == null) continue;
                String normalizedTransport = transport.trim().toLowerCase(Locale.ROOT);
                if (!("max".equals(normalizedTransport)
                        || "tgapi".equals(normalizedTransport)
                        || "telegram".equals(normalizedTransport)
                        || "whatsapp".equals(normalizedTransport))) continue;
                String key = normalizedTransport + ":" + chatId;
                String username = field(record, "user_username", "username");
                ContactService.ChatContactCandidate next = new ContactService.ChatContactCandidate(
                        transport,
                        field(record, "chat_id", "recipient_chat_id", "recipient.chat_id"),
                        username,
                        field(record, "user_phone", "phone"),
                        null);
                Instant timestamp = parseTimestamp(field(record, "datetime", "timestamp"));
                TimedCandidate current = candidates.get(key);
                if (current == null || timestamp.isAfter(current.timestamp())) {
                    candidates.put(key, new TimedCandidate(timestamp, next));
                }
            }
        }
        Set<String> existingChatKeys = contactService.getExistingChatKeys();
        List<ContactService.ChatContactCandidate> missing = candidates.values().stream()
                .map(TimedCandidate::candidate)
                .filter(candidate -> !existingChatKeys.contains(
                        normalizeTransport(candidate.chatType()) + ":" + candidate.chatId()
                ))
                .toList();
        missing.forEach(candidate -> pendingContactService.remember(
                candidate.chatType(), candidate.chatId(), null,
                candidate.username(), candidate.phone(), "MESSAGES_DUMP"
        ));
        return missing.size();
    }

    private Instant parseTimestamp(String value) {
        if (value == null) return Instant.EPOCH;
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return Instant.EPOCH;
        }
    }

    private String normalizeTransport(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "tgapi".equals(normalized) ? "telegram" : normalized;
    }

    public void requestSynchronization() {
        settingsService.requestMessagesSync();
    }


    private String field(CSVRecord record, String... names) {
        for (String expected : names) {
            for (String actual : record.toMap().keySet()) {
                if (actual.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).equals(expected)) {
                    String value = record.get(actual);
                    if (value != null && !value.isBlank()) return value.trim();
                }
            }
        }
        return null;
    }

    private record TimedCandidate(
            Instant timestamp,
            ContactService.ChatContactCandidate candidate
    ) {}
}
