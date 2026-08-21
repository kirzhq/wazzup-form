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
import ru.kirzhq.wazzup.client.WazzupRestClientFactory;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.exception.WazzupApiException;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URI;
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
    private static final int MAX_EXPORT_SIZE_BYTES = 100 * 1024 * 1024;
    private final PartnerOauthService oauthService;
    private final SettingsService settingsService;
    private final PendingContactService pendingContactService;
    private final ContactService contactService;
    private final WazzupPartnerProperties properties;
    private final RestClient restClient = WazzupRestClientFactory.create(
            "https://tech.wazzup24.com/v2"
    );
    private final RestClient downloadClient = WazzupRestClientFactory.create(null);

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
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new WazzupApiException("Wazzup вернул некорректную ссылку на выгрузку", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new WazzupApiException("Ссылка на выгрузку Wazzup должна использовать HTTPS");
        }

        return downloadClient.get().uri(uri).exchange((request, response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new WazzupApiException(
                        "Не удалось скачать выгрузку Wazzup: HTTP "
                                + response.getStatusCode().value()
                );
            }
            long contentLength = response.getHeaders().getContentLength();
            if (contentLength > MAX_EXPORT_SIZE_BYTES) {
                throw new WazzupApiException("Выгрузка Wazzup превышает 100 МБ");
            }
            try (InputStream input = response.getBody()) {
                byte[] bytes = input.readNBytes(MAX_EXPORT_SIZE_BYTES + 1);
                if (bytes.length == 0) {
                    throw new WazzupApiException("Wazzup вернул пустую выгрузку");
                }
                if (bytes.length > MAX_EXPORT_SIZE_BYTES) {
                    throw new WazzupApiException("Выгрузка Wazzup превышает 100 МБ");
                }
                return bytes;
            }
        });
    }

    private int importCsv(byte[] bytes) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
        Map<String, TimedCandidate> candidates = new LinkedHashMap<>();
        Map<String, TimedProfile> inboundProfiles = new LinkedHashMap<>();
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
                String normalizedTransport = normalizeTransport(transport);
                if (!("max".equals(normalizedTransport)
                        || "telegram".equals(normalizedTransport)
                        || "whatsapp".equals(normalizedTransport))) continue;
                String normalizedChatId = chatId.trim();
                if (!isPersonalChatId(normalizedTransport, normalizedChatId)) continue;
                String key = normalizedTransport + ":" + normalizedChatId;
                String username = field(record, "user_username", "username");
                ContactService.ChatContactCandidate next = new ContactService.ChatContactCandidate(
                        normalizedTransport,
                        normalizedChatId,
                        null,
                        null,
                        null);
                Instant timestamp = parseTimestamp(field(record, "datetime", "timestamp"));
                TimedCandidate current = candidates.get(key);
                if (current == null || timestamp.isAfter(current.timestamp())) {
                    candidates.put(key, new TimedCandidate(timestamp, next));
                }
                if (inbound) {
                    TimedProfile profile = new TimedProfile(
                            timestamp,
                            username,
                            field(record, "user_phone", "phone")
                    );
                    TimedProfile currentProfile = inboundProfiles.get(key);
                    if (currentProfile == null || timestamp.isAfter(currentProfile.timestamp())) {
                        inboundProfiles.put(key, profile);
                    }
                }
            }
        }
        Set<String> existingChatKeys = contactService.getExistingChatKeys();
        List<ContactService.ChatContactCandidate> missing = candidates.values().stream()
                .map(timed -> {
                    ContactService.ChatContactCandidate candidate = timed.candidate();
                    String key = normalizeTransport(candidate.chatType()) + ":" + candidate.chatId();
                    TimedProfile profile = inboundProfiles.get(key);
                    return new ContactService.ChatContactCandidate(
                            candidate.chatType(), candidate.chatId(),
                            profile == null ? null : profile.username(),
                            profile == null ? null : profile.phone(), null
                    );
                })
                .filter(candidate -> !existingChatKeys.contains(
                        normalizeTransport(candidate.chatType()) + ":" + candidate.chatId()
                ))
                .toList();
        missing.forEach(candidate -> {
            String key = normalizeTransport(candidate.chatType()) + ":" + candidate.chatId();
            pendingContactService.rememberFromDump(
                    candidate.chatType(), candidate.chatId(), candidate.username(), candidate.phone(),
                    candidates.get(key).timestamp()
            );
        });
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

    private boolean isPersonalChatId(String chatType, String chatId) {
        if (!chatId.matches("\\d+")) return false;
        if ("whatsapp".equals(chatType)) return chatId.length() >= 10 && chatId.length() <= 15;
        return chatId.length() <= 20;
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

    private record TimedProfile(Instant timestamp, String username, String phone) {}
}
