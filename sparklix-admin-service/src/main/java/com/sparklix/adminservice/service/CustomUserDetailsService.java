package com.sparklix.adminservice.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service("adminUserDetailsService") // Ensure this bean name matches @Qualifier in JwtAuthenticationFilter
public class CustomUserDetailsService {

    public UserDetails buildUserDetails(String username, List<String> rolesFromToken) {
        if (username == null || rolesFromToken == null) {
            throw new IllegalArgumentException("Username and roles must be provided to build UserDetails in admin-service");
        }
        List<GrantedAuthority> authorities = rolesFromToken.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Password is not relevant here as authentication happened via JWT
        return new User(username, "[PROTECTED_BY_TOKEN]", true, true, true, true, authorities);
    }
}