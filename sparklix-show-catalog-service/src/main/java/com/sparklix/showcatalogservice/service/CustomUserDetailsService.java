package com.sparklix.showcatalogservice.service; // Correct package

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service("catalogUserDetailsService") // Unique bean name
public class CustomUserDetailsService {

    public UserDetails buildUserDetails(String username, List<String> rolesFromToken) {
        if (username == null || rolesFromToken == null) {
            throw new IllegalArgumentException("Username and roles must be provided to build UserDetails in show-catalog-service");
        }
        List<GrantedAuthority> authorities = rolesFromToken.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        // Password is not relevant here for token-based authentication context setup
        return new User(username, "[PROTECTED_BY_TOKEN]", true, true, true, true, authorities);
    }
}