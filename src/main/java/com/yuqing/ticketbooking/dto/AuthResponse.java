package com.yuqing.ticketbooking.dto;

import com.yuqing.ticketbooking.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private UserRole role;
}
