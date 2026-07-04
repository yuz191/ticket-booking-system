package com.yuqing.ticketbooking.config;

import com.yuqing.ticketbooking.security.CustomerUserDetailService;
import com.yuqing.ticketbooking.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomerUserDetailService customerUserDetailService;


    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomerUserDetailService customerUserDetailService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customerUserDetailService = customerUserDetailService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                /*
                 * Disable CSRF protection.
                 *
                 * CSRF mainly protects traditional web apps that use cookies/sessions.
                 *
                 *
                 * We are using JWT + REST API, and the token is sent in the Authorization header,
                 * so we disable CSRF here.
                 */
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/ticket-types").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ticket-types").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception.
                        authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized.")))
                /*
                 * Set the authentication provider
                 * The authentication provider tells Spring Security:
                 * how to load a user by email
                 * how to verify the password
                */
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // build() creates the SecurityFilterChain bean based on the configuration above.
        return http.build();
    }

    /*
     * AuthenticationProvider performs the actual authentication logic.
     *
     * Here we use DaoAuthenticationProvider.
     *
     * DaoAuthenticationProvider will:
     *
     * 1. use customUserDetailsService to load the user by email
     *
     * 2. use passwordEncoder to verify the password
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customerUserDetailService);

        /*
         * Set the password encoding/verifying strategy.
         *
         * During login, Spring Security uses this PasswordEncoder
         * to compare the raw password entered by the user
         * with the encoded password stored in the database.
         */
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /*
     * AuthenticationManager is the entry point for authentication in Spring Security.
     *
     * In AuthService.login(), we call:
     *
     * authenticationManager.authenticate(...)
     *
     * It delegates the login request to the AuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /*
     * PasswordEncoder is used to encode and verify passwords.
     *
     * BCryptPasswordEncoder converts raw passwords into BCrypt hashes.
     *
     * During registration:
     *
     * passwordEncoder.encode(rawPassword)
     *
     * During login:
     *
     * passwordEncoder.matches(rawPassword, encodedPassword)
     *
     * Note: never store raw passwords in the database.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
