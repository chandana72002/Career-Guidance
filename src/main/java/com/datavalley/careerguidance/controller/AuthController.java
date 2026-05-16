package com.datavalley.careerguidance.controller;

import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datavalley.careerguidance.dto.AuthDtos;
import com.datavalley.careerguidance.service.AuthService;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        AuthDtos.AuthResponse response = authService.register(request);
        return toPayload(response);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.AuthResponse response = authService.login(request);
        return toPayload(response);
    }

    private Map<String, Object> toPayload(AuthDtos.AuthResponse response) {
        return Map.of(
            "token", response.token(),
            "user", Map.of(
                "fullName", response.fullName(),
                "email", response.email(),
                "role", response.role()
            )
        );
    }
}
