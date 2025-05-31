package com.sparklix.bookingservice.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service("bookingUserDetailsService") // Unique bean name
public class CustomUserDetailsService {
    public UserDetails buildUserDetails(String username, List<String> rolesFromToken) {
        if (username == null || rolesFromToken == null) {
            throw new IllegalArgumentException("Username and roles must be provided for UserDetails in booking-service");
        }
        List<GrantedAuthority> authorities = rolesFromToken.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new User(username, "[PROTECTED_BY_TOKEN]", true, true, true, true, authorities);
    }
}