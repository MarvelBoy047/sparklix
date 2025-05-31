package com.sparklix.paymentservice.config;

import com.sparklix.paymentservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sparklix.paymentservice.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order; // Ensure this is imported
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // For @PreAuthorize on controller methods
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Filter chain for public/service-to-service endpoints that don't need user JWT
    @Bean
    @Order(1) // Higher precedence
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/payments/webhook/razorpay", "/api/payments/create-order", "/api/payments/verify-signature", "/actuator/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // All paths matched by securityMatcher are permitted
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Still stateless
            )
            .exceptionHandling(exception -> exception // Still good to have an entry point for direct access issues
                .authenticationEntryPoint(authenticationEntryPoint)
            );
        // NO JWT filter for these specific paths if booking-service isn't sending a user token
        return http.build();
    }

    // Default filter chain for any other endpoint that needs JWT authentication
    @Bean
    @Order(2) // Lower precedence
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // Example: If you add other /api/payments/** endpoints that DO need user auth
                // .requestMatchers("/api/payments/user/**").authenticated() 
                .anyRequest().authenticated() // All other requests need authentication
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}