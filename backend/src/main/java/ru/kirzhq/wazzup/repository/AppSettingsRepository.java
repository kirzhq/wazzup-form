package ru.kirzhq.wazzup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirzhq.wazzup.entity.AppSettings;

public interface AppSettingsRepository
        extends JpaRepository<AppSettings, Long> {

    java.util.Optional<AppSettings> findFirstByOrderByIdAsc();
}
