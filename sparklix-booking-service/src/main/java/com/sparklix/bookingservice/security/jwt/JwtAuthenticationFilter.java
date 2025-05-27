package com.sparklix.bookingservice.security.jwt;

import com.sparklix.bookingservice.service.CustomUserDetailsService; 
import com.sparklix.bookingservice.util.JwtUtil; 

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Qualifier("bookingUserDetailsService") CustomUserDetailsService customUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                logger.debug("ADMIN-SERVICE: Processing JWT: {}", jwt.substring(0, Math.min(jwt.length(), 30)) + "..."); // Log partial JWT
                if (jwtUtil.validateTokenSignature(jwt)) { // Step 1: Validate signature is good (even if expired for this check)
                    logger.debug("ADMIN-SERVICE: JWT signature is valid.");
                    if (!jwtUtil.isTokenExpired(jwt)) { // Step 2: Explicitly check expiration
                        logger.debug("ADMIN-SERVICE: JWT is not expired.");
                        String username = jwtUtil.getUsernameFromToken(jwt);
                        List<String> rolesFromToken = jwtUtil.getRolesFromToken(jwt); 

                        logger.debug("ADMIN-SERVICE: Extracted username: '{}', roles: {}", username, rolesFromToken);

                        if (username != null && rolesFromToken != null && !rolesFromToken.isEmpty()) {
                            // Build UserDetails using the information from the token
                            UserDetails userDetails = customUserDetailsService.buildUserDetails(username, rolesFromToken);
                            
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.info("ADMIN-SERVICE: Successfully set Authentication in SecurityContext for user '{}', roles: {}", username, rolesFromToken);
                        } else {
                            logger.warn("ADMIN-SERVICE: Username or roles not found/empty in token, although signature is valid and token not expired.");
                        }
                    } else {
                         logger.warn("ADMIN-SERVICE: JWT token IS EXPIRED. URI: {}", request.getRequestURI());
                    }
                } else {
                     logger.warn("ADMIN-SERVICE: JWT token signature validation FAILED. URI: {}", request.getRequestURI());
                }
            } else {
                logger.trace("ADMIN-SERVICE: No JWT token found in request to URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("ADMIN-SERVICE: Error in JwtAuthenticationFilter: {}", e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}