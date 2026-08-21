package ru.kirzhq.wazzup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_webhooks")
public class ProcessedWebhook {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedWebhook() {}

    public ProcessedWebhook(String id, Instant processedAt) {
        this.id = id;
        this.processedAt = processedAt;
    }

    public String getId() { return id; }
    public Instant getProcessedAt() { return processedAt; }
}
