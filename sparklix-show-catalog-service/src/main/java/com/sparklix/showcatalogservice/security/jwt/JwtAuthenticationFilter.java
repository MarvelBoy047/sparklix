package com.sparklix.showcatalogservice.security.jwt; // Correct package

import com.sparklix.showcatalogservice.service.CustomUserDetailsService; 
import com.sparklix.showcatalogservice.util.JwtUtil; 

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Qualifier("catalogUserDetailsService") CustomUserDetailsService customUserDetailsService) {
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
                logger.debug("SHOW-CATALOG-SERVICE: Processing JWT...");
                // First, validate the signature. If it's invalid, no point in checking expiration or extracting claims.
                if (jwtUtil.validateTokenSignature(jwt)) {
                    // If signature is valid, then check for expiration.
                    if (!jwtUtil.isTokenExpired(jwt)) {
                        String username = jwtUtil.getUsernameFromToken(jwt);
                        List<String> rolesFromToken = jwtUtil.getRolesFromToken(jwt);
                        logger.debug("SHOW-CATALOG-SERVICE: Extracted username: '{}', roles: {}", username, rolesFromToken);
                        
                        if (username != null && !username.isEmpty() && rolesFromToken != null && !rolesFromToken.isEmpty()) {
                            UserDetails userDetails = customUserDetailsService.buildUserDetails(username, rolesFromToken);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.info("SHOW-CATALOG-SERVICE: Set Authentication for user '{}', roles: {}", username, rolesFromToken);
                        } else {
                            logger.warn("SHOW-CATALOG-SERVICE: Username or roles not found/empty in token. Username: '{}', Roles: {}", username, rolesFromToken);
                        }
                    } else {
                         logger.warn("SHOW-CATALOG-SERVICE: JWT token IS EXPIRED. URI: {}", request.getRequestURI());
                    }
                } else {
                     logger.warn("SHOW-CATALOG-SERVICE: JWT token signature validation FAILED. URI: {}", request.getRequestURI());
                }
            } else {
                logger.trace("SHOW-CATALOG-SERVICE: No JWT token found for URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("SHOW-CATALOG-SERVICE: Error in JwtAuthenticationFilter: {}", e.getMessage(), e);
             // Ensure filter chain continues even if an unexpected error occurs in our logic
             // to prevent requests from hanging, though security context won't be set.
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