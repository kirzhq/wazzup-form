package ru.kirzhq.wazzup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirzhq.wazzup.entity.PendingContactCandidate;

import java.util.List;
import java.util.Optional;

public interface PendingContactCandidateRepository extends JpaRepository<PendingContactCandidate, String> {
    Optional<PendingContactCandidate> findByChatTypeAndChatId(String chatType, String chatId);
    List<PendingContactCandidate> findAllByStatusOrderByUpdatedAtDesc(String status);
}
