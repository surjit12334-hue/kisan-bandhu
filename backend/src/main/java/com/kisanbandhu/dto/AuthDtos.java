package com.kisanbandhu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Pattern(regexp = "\\d{10}", message = "phone must be 10 digits") String phone,
            @NotBlank @Size(min = 4, max = 72) String password,
            String language,
            Boolean featurePhone) {}

    public record LoginRequest(
            @NotBlank String phone,
            @NotBlank String password) {}

    public record OtpRequest(
            @NotBlank @Pattern(regexp = "\\d{10}") String phone,
            @NotBlank String purpose) {}

    public record OtpVerifyRequest(
            @NotBlank @Pattern(regexp = "\\d{10}") String phone,
            @NotBlank String code,
            @NotBlank String purpose) {}

    public record AuthResponse(String token, ProfileResponse user) {}

    public record ProfileResponse(Long id, String name, String phone, String role,
                                  String language, boolean featurePhone) {}
}
