package com.sparklix.paymentservice.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List; // Changed from Set
import java.util.Set;
import java.util.stream.Collectors;

@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // This service primarily validates tokens; direct username lookup isn't its main role here.
        // This method might not be hit if JwtAuthenticationFilter directly creates UserDetails.
        throw new UsernameNotFoundException("User lookup by username not supported in this service; token validation is primary.");
    }

    // This method is called by JwtAuthenticationFilter after extracting username and roles from a valid token.
    public UserDetails buildUserDetails(String username, List<String> roles) { // Changed to List<String>
        Set<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        // For token-based authentication, the password field in UserDetails is not used for validation here.
        return new User(username, "", authorities);
    }
}