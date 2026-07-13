package com.yuqing.ticketbooking.service;

import com.yuqing.ticketbooking.dto.AuthResponse;
import com.yuqing.ticketbooking.dto.LoginRequest;
import com.yuqing.ticketbooking.dto.RegisterRequest;
import com.yuqing.ticketbooking.entity.User;
import com.yuqing.ticketbooking.entity.UserRole;
import com.yuqing.ticketbooking.repository.UserRepository;
import com.yuqing.ticketbooking.security.JwtService;
import jakarta.validation.Valid;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(@Valid RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(@Valid LoginRequest request) {
        /*
         * Ask Spring Security to authenticate the user with the provided email and password.
         * This will use the configured AuthenticationManager, AuthenticationProvider,
         * CustomUserDetailsService, and PasswordEncoder behind the scenes.
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
            )
        );

        /*
         * If authentication fails, Spring Security will throw an exception,
         * so the code below will not run.
         */
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        /*
         * Generate a JWT token for the authenticated user.
         * The token will include the user's email as the subject
         * and the user's role as a custom claim.
        */
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole()
        );

    }
}
