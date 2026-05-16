package com.datavalley.careerguidance.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        String fullName,
        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,
        String role
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(
        @Email(message = "Valid email is required")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Password is required")
        String password
    ) {
    }

    public record AuthResponse(
        String token,
        String fullName,
        String email,
        String role
    ) {
    }
}
