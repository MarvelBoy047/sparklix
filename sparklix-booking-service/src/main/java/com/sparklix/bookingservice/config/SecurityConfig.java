package com.sparklix.bookingservice.config;

import com.sparklix.bookingservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sparklix.bookingservice.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier; // <<<--- ADD THIS IMPORT
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
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // No @Autowired needed here as it's the only constructor
    public SecurityConfig(@Qualifier("bookingAuthEntryPoint") JwtAuthenticationEntryPoint unauthorizedHandler,
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
            .anonymous(anonymous -> anonymous.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                // Booking actions need USER or VENDOR role
                .requestMatchers(HttpMethod.POST, "/api/bookings").hasAnyRole("USER", "VENDOR")
                .requestMatchers(HttpMethod.GET, "/api/bookings/my-bookings").hasAnyRole("USER", "VENDOR")
                .requestMatchers(HttpMethod.GET, "/api/bookings/{bookingId}").hasAnyRole("USER", "VENDOR")
                // Example: an admin might view all bookings, but not part of current scope for this controller
                // .requestMatchers(HttpMethod.GET, "/api/bookings/admin/all").hasRole("ADMIN") 
                .anyRequest().authenticated() 
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}