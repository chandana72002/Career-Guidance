package com.datavalley.careerguidance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.datavalley.careerguidance.dto.AuthDtos;
import com.datavalley.careerguidance.entity.Role;
import com.datavalley.careerguidance.entity.User;
import com.datavalley.careerguidance.entity.UserProfile;
import com.datavalley.careerguidance.repository.UserProfileRepository;
import com.datavalley.careerguidance.repository.UserRepository;
import com.datavalley.careerguidance.security.JwtService;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthService authService = new AuthService(
        userRepository,
        userProfileRepository,
        passwordEncoder,
        jwtService,
        authenticationManager
    );

    @Test
    void shouldPersistRequestedCounselorRoleDuringRegistration() {
        when(userRepository.existsByEmail("counselor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDtos.AuthResponse response = authService.register(
            new AuthDtos.RegisterRequest("Counselor One", "counselor@example.com", "secret123", "COUNSELOR")
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.ROLE_COUNSELOR);
        assertThat(response.role()).isEqualTo("ROLE_COUNSELOR");
    }

    @Test
    void shouldDefaultToStudentRoleWhenRegistrationRoleIsMissing() {
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthDtos.AuthResponse response = authService.register(
            new AuthDtos.RegisterRequest("Student One", "student@example.com", "secret123", null)
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(response.role()).isEqualTo("ROLE_USER");
    }
}
