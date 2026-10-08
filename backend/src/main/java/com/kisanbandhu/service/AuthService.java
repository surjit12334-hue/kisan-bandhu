package com.kisanbandhu.service;

import com.kisanbandhu.domain.Role;
import com.kisanbandhu.domain.User;
import com.kisanbandhu.dto.AuthDtos.*;
import com.kisanbandhu.repo.UserRepo;
import com.kisanbandhu.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuditService auditService;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtService jwtService,
                       OtpService otpService, AuditService auditService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByPhone(req.phone())) {
            throw new IllegalArgumentException("Phone number already registered");
        }
        User user = new User();
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setLanguage(req.language() == null || req.language().isBlank() ? "hi" : req.language());
        user.setFeaturePhone(Boolean.TRUE.equals(req.featurePhone()));
        user.setRole(Role.FARMER);
        userRepo.save(user);
        auditService.log("REGISTER", "New farmer registered: " + user.getName(), user.getPhone());
        return new AuthResponse(jwtService.issue(user), profile(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByPhone(req.phone())
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid phone or password");
        }
        return new AuthResponse(jwtService.issue(user), profile(user));
    }

    @Transactional
    public ProfileResponse requestOtp(OtpRequest req) {
        otpService.request(req.phone(), req.purpose());
        User user = userRepo.findByPhone(req.phone()).orElse(null);
        return user == null ? null : profile(user);
    }

    @Transactional
    public boolean verifyOtp(OtpVerifyRequest req) {
        return otpService.verify(req.phone(), req.code(), req.purpose());
    }

    public User requireUser(String phone) {
        return userRepo.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + phone));
    }

    public static ProfileResponse profile(User user) {
        return new ProfileResponse(user.getId(), user.getName(), user.getPhone(),
                user.getRole().name(), user.getLanguage(), user.isFeaturePhone());
    }
}
