package com.sparklix.userservice.security.jwt;

import com.sparklix.userservice.service.CustomUserDetailsService;
import com.sparklix.userservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtil.validateTokenSignature(jwt)) { // Validate signature first
                String username = jwtUtil.getUsernameFromToken(jwt);

                // Load user details from the database
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // Further validation: ensure token subject matches UserDetails and token is not expired for this user context
                if (jwtUtil.validateToken(jwt, userDetails)) { // This checks username match and expiration
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails,
                                                                    null, // Credentials are not needed here as JWT is the credential
                                                                    userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set the authentication in the SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Set Authentication in SecurityContext for user '{}'", username);
                } else {
                     logger.warn("JWT token validation failed for user '{}' (e.g. username mismatch or expired for context)", username);
                }
            } else {
                if (jwt != null) {
                    logger.warn("JWT token signature validation failed or token is malformed.");
                }
                // If no JWT or JWT signature is invalid, just continue the filter chain without setting authentication.
                // Access to secured endpoints will be denied by subsequent security mechanisms.
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage());
            // e.printStackTrace(); // For more detailed debugging
        }

        filterChain.doFilter(request, response); // Continue the filter chain
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Extract token part after "Bearer "
        }
        logger.trace("No JWT token found in Authorization header or does not start with Bearer string");
        return null;
    }
}