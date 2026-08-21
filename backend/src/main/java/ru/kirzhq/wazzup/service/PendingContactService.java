package ru.kirzhq.wazzup.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.kirzhq.wazzup.dto.PendingContactResponse;
import ru.kirzhq.wazzup.entity.PendingContactCandidate;
import ru.kirzhq.wazzup.repository.PendingContactCandidateRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PendingContactService {
    private static final String PENDING = "PENDING";
    private final PendingContactCandidateRepository repository;
    private final ContactService contactService;

    public PendingContactService(
            PendingContactCandidateRepository repository,
            ContactService contactService
    ) {
        this.repository = repository;
        this.contactService = contactService;
    }

    @Transactional
    public void remember(
            String chatType,
            String chatId,
            String name,
            String username,
            String phone,
            String source
    ) {
        if (!StringUtils.hasText(chatType) || !StringUtils.hasText(chatId)) return;
        String normalizedType = normalizeChatType(chatType);
        if (normalizedType.endsWith("group")) return;

        var existing = repository.findByChatTypeAndChatId(normalizedType, chatId.trim());
        PendingContactCandidate candidate = existing
                .orElseGet(() -> {
                    PendingContactCandidate created = new PendingContactCandidate();
                    created.setId(UUID.randomUUID().toString());
                    created.setChatType(normalizedType);
                    created.setChatId(chatId.trim());
                    return created;
                });
        candidate.setName(prefer(name, candidate.getName()));
        candidate.setUsername(prefer(username, candidate.getUsername()));
        candidate.setPhone(prefer(normalizePhone(phone), candidate.getPhone()));
        candidate.setSource(source);
        if (existing.isEmpty()) candidate.setStatus(PENDING);
        candidate.setUpdatedAt(Instant.now());
        repository.save(candidate);
    }

    public List<PendingContactResponse> getPending() {
        return repository.findAllByStatusOrderByUpdatedAtDesc(PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public boolean approve(String id, String name) {
        PendingContactCandidate candidate = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Кандидат не найден"));
        boolean created = contactService.ensureChatContact(
                candidate.getChatType(),
                candidate.getChatId(),
                candidate.getUsername(),
                candidate.getPhone(),
                name.trim()
        );
        candidate.setStatus(created ? "APPROVED" : "ALREADY_EXISTS");
        candidate.setUpdatedAt(Instant.now());
        repository.save(candidate);
        return created;
    }

    @Transactional
    public void dismiss(String id) {
        PendingContactCandidate candidate = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Кандидат не найден"));
        candidate.setStatus("DISMISSED");
        candidate.setUpdatedAt(Instant.now());
        repository.save(candidate);
    }

    private PendingContactResponse toResponse(PendingContactCandidate value) {
        return new PendingContactResponse(
                value.getId(), value.getChatType(), value.getChatId(), value.getName(),
                value.getUsername(), value.getPhone(), value.getSource(), value.getUpdatedAt()
        );
    }

    private String normalizeChatType(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "tgapi".equals(normalized) ? "telegram" : normalized;
    }

    private String normalizePhone(String value) {
        if (!StringUtils.hasText(value)) return null;
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private String prefer(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
