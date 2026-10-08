package com.kisanbandhu.service;

import com.kisanbandhu.domain.OtpCode;
import com.kisanbandhu.repo.OtpRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    public static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final OtpRepo otpRepo;
    private final SmsService smsService;

    public OtpService(OtpRepo otpRepo, SmsService smsService) {
        this.otpRepo = otpRepo;
        this.smsService = smsService;
    }

    @Transactional
    public String request(String phone, String purpose) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setCode(code);
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plus(TTL));
        otpRepo.save(otp);
        smsService.send(phone, "Kisan Bandhu OTP: " + code + " (valid 5 min). " + purpose);
        return code;
    }

    @Transactional
    public boolean verify(String phone, String code, String purpose) {
        return otpRepo.findByPhoneOrderByExpiresAtDesc(phone).stream()
                .filter(o -> o.getPurpose().equals(purpose))
                .filter(o -> o.getVerifiedAt() == null)
                .findFirst()
                .map(o -> {
                    if (o.getExpiresAt().isBefore(LocalDateTime.now())) return false;
                    if (o.getAttempts() >= MAX_ATTEMPTS) return false;
                    o.setAttempts(o.getAttempts() + 1);
                    if (o.getCode().equals(code)) {
                        o.setVerifiedAt(LocalDateTime.now());
                        otpRepo.save(o);
                        return true;
                    }
                    otpRepo.save(o);
                    return false;
                })
                .orElse(false);
    }
}
