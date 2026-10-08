package com.kisanbandhu.repo;

import com.kisanbandhu.domain.WaitlistEntry;
import com.kisanbandhu.domain.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WaitlistRepo extends JpaRepository<WaitlistEntry, Long> {
    List<WaitlistEntry> findByMandiIdAndPreferredDateAndStatusOrderByCreatedAt(
            Long mandiId, LocalDate preferredDate, WaitlistStatus status);
    long countByMandiIdAndStatus(Long mandiId, WaitlistStatus status);
}
