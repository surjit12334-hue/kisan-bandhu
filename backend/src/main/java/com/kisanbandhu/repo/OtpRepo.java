package com.kisanbandhu.repo;

import com.kisanbandhu.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpRepo extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findFirstByPhoneAndPurposeOrderByExpiresAtDesc(String phone, String purpose);
    List<OtpCode> findByPhoneOrderByExpiresAtDesc(String phone);
}
