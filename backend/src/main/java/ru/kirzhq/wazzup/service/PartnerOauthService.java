package ru.kirzhq.wazzup.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;
import ru.kirzhq.wazzup.dto.OauthStartResponse;
import ru.kirzhq.wazzup.dto.PartnerStatusResponse;
import ru.kirzhq.wazzup.dto.PartnerTokenData;
import ru.kirzhq.wazzup.dto.PartnerTokenResponse;
import ru.kirzhq.wazzup.entity.AppSettings;
import ru.kirzhq.wazzup.exception.WazzupApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class PartnerOauthService {
    private static final long EXPIRY_SAFETY_SECONDS = 60;

    private final WazzupPartnerProperties properties;
    private final SettingsService settingsService;
    private final SecretEncryptionService encryptionService;
    private final RestClient restClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerOauthService(
            WazzupPartnerProperties properties,
            SettingsService settingsService,
            SecretEncryptionService encryptionService
    ) {
        this.properties = properties;
        this.settingsService = settingsService;
        this.encryptionService = encryptionService;
        this.restClient = RestClient.builder()
                .baseUrl("https://tech.wazzup24.com/v2")
                .build();
    }

    public PartnerStatusResponse getStatus() {
        boolean connected = false;
        if (settingsService.isConfigured()) {
            AppSettings settings = settingsService.getSettings();
            connected = settings.getPartnerRefreshToken() != null;
        }
        return new PartnerStatusResponse(properties.isConfigured(), connected);
    }

    public OauthStartResponse startAuthorization() {
        requireConfiguration();

        String verifier = randomUrlSafe(64);
        String state = randomUrlSafe(32);
        String challenge = sha256UrlSafe(verifier);
        settingsService.saveOauthRequest(state, encryptionService.encrypt(verifier));

        String url = UriComponentsBuilder
                .fromUriString("https://tech.wazzup24.com/v2/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", "transport,crm")
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();

        return new OauthStartResponse(url);
    }

    public void completeAuthorization(String code, String state) {
        requireConfiguration();
        AppSettings settings = settingsService.getSettings();

        if (state == null || !state.equals(settings.getOauthState())) {
            throw new IllegalArgumentException("Некорректный OAuth state");
        }
        if (settings.getOauthCodeVerifier() == null) {
            throw new IllegalStateException("OAuth-авторизация не была начата");
        }

        String verifier = encryptionService.decrypt(settings.getOauthCodeVerifier());
        PartnerTokenData tokens = requestTokens(Map.of(
                "grant_type", "authorization_code",
                "authorize_code_data", Map.of(
                        "code", code,
                        "redirect_uri", properties.redirectUri(),
                        "client_id", properties.clientId(),
                        "code_verifier", verifier
                )
        ));
        saveTokens(tokens);
    }

    public synchronized String getAccessToken() {
        requireConfiguration();
        AppSettings settings = settingsService.getSettings();
        if (settings.getPartnerRefreshToken() == null) {
            throw new IllegalStateException("Технический API Wazzup ещё не подключён");
        }

        Instant expiresAt = settings.getPartnerTokenExpiresAt();
        if (settings.getPartnerAccessToken() != null
                && expiresAt != null
                && expiresAt.isAfter(Instant.now().plusSeconds(EXPIRY_SAFETY_SECONDS))) {
            return encryptionService.decrypt(settings.getPartnerAccessToken());
        }

        String refreshToken = encryptionService.decrypt(settings.getPartnerRefreshToken());
        PartnerTokenData tokens = requestTokens(Map.of(
                "grant_type", "refresh_token",
                "refresh_token_data", Map.of(
                        "refresh_token", refreshToken,
                        "client_id", properties.clientId()
                )
        ));
        saveTokens(tokens);
        return tokens.accessToken();
    }

    private PartnerTokenData requestTokens(Map<String, Object> body) {
        try {
            PartnerTokenResponse response = restClient.post()
                    .uri("/oauth/token")
                    .headers(headers -> headers.setBasicAuth(
                            properties.email(),
                            properties.password(),
                            StandardCharsets.UTF_8
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PartnerTokenResponse.class);

            if (response == null || response.data() == null
                    || response.data().accessToken() == null
                    || response.data().refreshToken() == null) {
                throw new WazzupApiException("Wazzup не вернул OAuth-токены");
            }
            return response.data();
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось получить токены технического API Wazzup",
                    exception
            );
        }
    }

    private void saveTokens(PartnerTokenData tokens) {
        settingsService.savePartnerTokens(
                encryptionService.encrypt(tokens.accessToken()),
                encryptionService.encrypt(tokens.refreshToken()),
                Instant.now().plusSeconds(tokens.expiresIn())
        );
    }

    private void requireConfiguration() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Не настроены переменные технического API Wazzup"
            );
        }
    }

    private String randomUrlSafe(int bytesCount) {
        byte[] bytes = new byte[bytesCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256UrlSafe(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось сформировать PKCE challenge", exception);
        }
    }
}
