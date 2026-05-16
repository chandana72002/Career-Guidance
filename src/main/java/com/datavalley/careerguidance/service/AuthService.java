package com.datavalley.careerguidance.service;

import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.datavalley.careerguidance.dto.AuthDtos;
import com.datavalley.careerguidance.entity.Role;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.exception.BadRequestException;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserRepository;
import com.datavalley.careerguidance.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(resolveRequestedRole(request.role()));
        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        userProfileRepository.save(profile);

        return buildResponse(savedUser);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> new BadRequestException("User not found"));
        return buildResponse(user);
    }

    private AuthDtos.AuthResponse buildResponse(User user) {
        return new AuthDtos.AuthResponse(
            jwtService.generateToken(user),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    private Role resolveRequestedRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.ROLE_USER;
        }

        return switch (requestedRole.trim().toUpperCase(Locale.ROOT)) {
            case "USER", "STUDENT", "ROLE_USER" -> Role.ROLE_USER;
            case "ADMIN", "ROLE_ADMIN" -> Role.ROLE_ADMIN;
            case "COUNSELOR", "COUNSELLOR", "CONSELOR", "ROLE_COUNSELOR" -> Role.ROLE_COUNSELOR;
            default -> throw new BadRequestException("Invalid role selected");
        };
    }
}
