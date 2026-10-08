package com.kisanbandhu.repo;

import com.kisanbandhu.domain.Mandi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MandiRepo extends JpaRepository<Mandi, Long> {
    Optional<Mandi> findByCode(String code);
}
