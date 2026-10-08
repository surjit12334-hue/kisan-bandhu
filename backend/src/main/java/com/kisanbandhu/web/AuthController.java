package com.kisanbandhu.web;

import com.kisanbandhu.dto.AuthDtos.*;
import com.kisanbandhu.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/auth/me")
    public ProfileResponse me() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return authService.profile(authService.requireUser(phone));
    }

    @PostMapping("/otp/request")
    public Map<String, String> requestOtp(@Valid @RequestBody OtpRequest req) {
        authService.requestOtp(req);
        return Map.of("status", "sent", "hint", "Mock SMS gateway: check server log / admin SMS outbox");
    }

    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        boolean ok = authService.verifyOtp(req);
        return Map.of("verified", ok);
    }
}
