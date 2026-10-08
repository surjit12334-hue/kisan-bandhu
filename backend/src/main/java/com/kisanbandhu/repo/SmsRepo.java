package com.kisanbandhu.repo;

import com.kisanbandhu.domain.SmsOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsRepo extends JpaRepository<SmsOutbox, Long> {
    List<SmsOutbox> findTop50ByOrderByCreatedAtDesc();
}
