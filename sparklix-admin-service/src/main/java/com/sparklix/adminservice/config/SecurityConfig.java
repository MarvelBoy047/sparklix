package com.sparklix.adminservice.config;

import com.fasterxml.jackson.databind.ObjectMapper; // Import
import com.sparklix.adminservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sparklix.adminservice.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.ServletException; // Import
import jakarta.servlet.http.HttpServletRequest; // Import
import jakarta.servlet.http.HttpServletResponse; // Import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus; // Import
import org.springframework.http.MediaType; // Import
import org.springframework.security.access.AccessDeniedException; // Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler; // Import
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException; // Import
import java.time.LocalDateTime; // Import
import java.util.HashMap; // Import
import java.util.Map; // Import

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() { // Define custom AccessDeniedHandler
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpStatus.FORBIDDEN.value());
            body.put("error", "Forbidden");
            body.put("message", "ADMIN-SERVICE: Access Denied. You do not have the required role.");
            body.put("path", request.getServletPath());

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(customAccessDeniedHandler()) // <--- CONFIGURE CUSTOM AccessDeniedHandler
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .anonymous(anonymous -> anonymous.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                    .requestMatchers("/error").permitAll() // <--- Explicitly permit /error to avoid re-auth
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}