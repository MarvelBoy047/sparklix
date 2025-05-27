package com.sparklix.showcatalogservice.config;

import com.sparklix.showcatalogservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sparklix.showcatalogservice.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // To enable @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .anonymous(anonymous -> anonymous.disable()) // No anonymous access by default
            .authorizeHttpRequests(auth -> auth
                // Publicly accessible GET endpoints for shows and reviews
                .requestMatchers(HttpMethod.GET, "/api/shows/**").permitAll() 
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // Any other request to /api/shows requires authentication (will be caught by @PreAuthorize on POST review)
                // Or specifically protect POST for reviews here if not using @PreAuthorize in controller:
                // .requestMatchers(HttpMethod.POST, "/api/shows/*/reviews").hasAnyRole("USER", "VENDOR")
                .anyRequest().authenticated() // Default to deny if not specified above
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}