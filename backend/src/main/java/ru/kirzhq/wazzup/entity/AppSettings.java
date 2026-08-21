package ru.kirzhq.wazzup.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key", nullable = false, length = 4096)
    private String apiKey;

    @Column(name = "partner_access_token", length = 4096)
    private String partnerAccessToken;

    @Column(name = "partner_refresh_token", length = 4096)
    private String partnerRefreshToken;

    @Column(name = "partner_token_expires_at")
    private java.time.Instant partnerTokenExpiresAt;

    @Column(name = "oauth_state", length = 200)
    private String oauthState;

    @Column(name = "oauth_code_verifier", length = 4096)
    private String oauthCodeVerifier;

    @Column(name = "messages_export_id", length = 100)
    private String messagesExportId;

    @Column(name = "messages_sync_started_at")
    private java.time.Instant messagesSyncStartedAt;

    @Column(name = "messages_last_synced_at")
    private java.time.Instant messagesLastSyncedAt;

    public AppSettings() {
    }

    public AppSettings(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getId() {
        return id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPartnerAccessToken() {
        return partnerAccessToken;
    }

    public void setPartnerAccessToken(String partnerAccessToken) {
        this.partnerAccessToken = partnerAccessToken;
    }

    public String getPartnerRefreshToken() {
        return partnerRefreshToken;
    }

    public void setPartnerRefreshToken(String partnerRefreshToken) {
        this.partnerRefreshToken = partnerRefreshToken;
    }

    public java.time.Instant getPartnerTokenExpiresAt() {
        return partnerTokenExpiresAt;
    }

    public void setPartnerTokenExpiresAt(java.time.Instant partnerTokenExpiresAt) {
        this.partnerTokenExpiresAt = partnerTokenExpiresAt;
    }

    public String getOauthState() {
        return oauthState;
    }

    public void setOauthState(String oauthState) {
        this.oauthState = oauthState;
    }

    public String getOauthCodeVerifier() {
        return oauthCodeVerifier;
    }

    public void setOauthCodeVerifier(String oauthCodeVerifier) {
        this.oauthCodeVerifier = oauthCodeVerifier;
    }

    public String getMessagesExportId() { return messagesExportId; }
    public void setMessagesExportId(String messagesExportId) { this.messagesExportId = messagesExportId; }
    public java.time.Instant getMessagesSyncStartedAt() { return messagesSyncStartedAt; }
    public void setMessagesSyncStartedAt(java.time.Instant value) { this.messagesSyncStartedAt = value; }
    public java.time.Instant getMessagesLastSyncedAt() { return messagesLastSyncedAt; }
    public void setMessagesLastSyncedAt(java.time.Instant value) { this.messagesLastSyncedAt = value; }
}
