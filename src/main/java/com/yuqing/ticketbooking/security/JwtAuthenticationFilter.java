package com.yuqing.ticketbooking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomerUserDetailService customerUserDetailService;


    public JwtAuthenticationFilter(JwtService jwtService, CustomerUserDetailService customerUserDetailService) {
        this.jwtService = jwtService;
        this.customerUserDetailService = customerUserDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // If the request does not have an Authorization header,
        // or the header does not start with "Bearer ",
        // it means this request does not carry a JWT.
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return;
        }

        // remove the "Bearer " prefix and keep only the JWT token
        String token = authHeader.substring(7);

        // If token is invalid, continue the filter chain.
        // If the request is for a protected API, Spring Security will reject it later.
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the email from the token.
        // When generating the token, we stored the email as the subject:
        // Jwts.builder().subject(email)
        // So here we can retrieve it using extractEmail(token)
        String email = jwtService.extractEmail(token);

        // SecurityContextHolder stores the authentication for the current request
        // If the current request does not already have authentication,
        // and the token contains an email,
        // we load the user details by email.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customerUserDetailService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            // Add extra request details to the authentication object,
            // such as IP address and session id.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Put the authentication object into the SecurityContext.
            // After this step, Spring Security considers:
            // the current request is authenticated
            // Later, in Service or Controller, we can get the current user:
            // SecurityContextHolder.getContext().getAuthentication().getName()
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // Continue the remaining filters, and eventually the request reaches the Controller
        // If we forget to call this, the request will stop here and never continue.
        filterChain.doFilter(request, response);
    }
}
