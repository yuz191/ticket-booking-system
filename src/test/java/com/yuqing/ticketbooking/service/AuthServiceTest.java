package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.AuthResponse;
import com.yuqing.ticketbooking.dto.LoginRequest;
import com.yuqing.ticketbooking.dto.RegisterRequest;
import com.yuqing.ticketbooking.entity.User;
import com.yuqing.ticketbooking.entity.UserRole;
import com.yuqing.ticketbooking.repository.UserRepository;
import com.yuqing.ticketbooking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void registerShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());

        User savedUser = savedUserCaptor.getValue();
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals("jwt-token", response.getToken());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("Email already registered.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void loginShouldAuthenticateUserAndReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice@example.com", "ADMIN")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                eq(new UsernamePasswordAuthenticationToken("alice@example.com", "secret123"))
        );
        assertEquals("jwt-token", response.getToken());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(UserRole.ADMIN, response.getRole());
    }
}
