package com.sparklix.userservice.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private List<String> roles;
    private String message; // Optional message

    // Constructor for JWT based response
    public AuthResponse(String token, String username, List<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = roles;
    }

    // Constructor for simple message response (e.g., if JWT is not yet implemented)
    public AuthResponse(String message) {
        this.message = message;
    }
}