package ru.kirzhq.wazzup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirzhq.wazzup.entity.ProcessedWebhook;

import java.time.Instant;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, String> {
    long deleteByProcessedAtBefore(Instant cutoff);
}
