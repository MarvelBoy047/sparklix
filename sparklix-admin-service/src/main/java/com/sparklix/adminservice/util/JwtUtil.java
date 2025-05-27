package com.sparklix.adminservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${sparklix.app.jwtSecret}")
    private String jwtSecretString;

    private Key key;

    @PostConstruct
    public void init() {
        if (jwtSecretString == null || jwtSecretString.getBytes().length < 32) {
            logger.error("ADMIN-SERVICE CRITICAL: JWT Secret key for token validation is not defined or too short in properties!");
            // Fallback to a dummy key to allow startup but ensure validation will fail if not configured.
            this.key = Keys.hmacShaKeyFor("EnsureThisIsConfiguredProperlyInProperties32Bytes!".getBytes());
        } else {
            byte[] keyBytes = jwtSecretString.getBytes();
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("ADMIN-SERVICE: JWT Secret Key initialized successfully.");
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) {
            logger.debug("ADMIN-SERVICE: Cannot get roles, claims are null for token.");
            return new ArrayList<>();
        }
        String rolesString = claims.get("roles", String.class); // "roles" is the claim name used in user-service
        if (rolesString != null && !rolesString.isEmpty()) {
            return Arrays.asList(rolesString.split(","));
        }
        logger.debug("ADMIN-SERVICE: No 'roles' claim found or claim is empty in token.");
        return new ArrayList<>();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        if (claims == null) return null;
        try {
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.trace("ADMIN-SERVICE: Could not apply claims resolver for token: {}", e.getMessage());
            return null;
        }
    }

    private Claims getAllClaimsFromToken(String token) { // For validation, we need to parse strictly
        try {
            return Jwts.parserBuilder()
                       .setSigningKey(key)
                       .build()
                       .parseClaimsJws(token)
                       .getBody();
        } catch (ExpiredJwtException e) {
            // For validation, an expired token's claims might still be needed to check subject before confirming expiration
            logger.warn("ADMIN-SERVICE: JWT token is expired when extracting claims: {}", e.getMessage());
            return e.getClaims(); 
        } catch (JwtException e) { // Catches Malformed, Signature, Unsupported, etc.
            logger.warn("ADMIN-SERVICE: Could not parse JWT claims due to: {}. Token: {}", e.getMessage(), token);
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token); // Use existing method that handles expired exception for claims
            if (claims == null) return true; // If claims couldn't be parsed at all (e.g. malformed, not just expired)
            
            final Date expiration = claims.getExpiration();
            if (expiration == null) {
                logger.warn("ADMIN-SERVICE: Token has no expiration claim.");
                return true; 
            }
            boolean expired = expiration.before(new Date());
            if (expired) {
                logger.warn("ADMIN-SERVICE: Token confirmed expired. Expiration: {}, Current: {}", expiration, new Date());
            }
            return expired;
        } catch (Exception e) { // Catch all for safety during expiration check
             logger.error("ADMIN-SERVICE: Error during isTokenExpired check", e);
             return true;
        }
    }

    public boolean validateTokenSignature(String token) {
        if (token == null) return false;
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            logger.debug("ADMIN-SERVICE: JWT signature is valid.");
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("ADMIN-SERVICE: Invalid JWT token structure (Malformed): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("ADMIN-SERVICE: JWT token is EXPIRED (but signature might be ok, checked during signature validation): {}", e.getMessage());
            return true; // Signature is considered valid even if token is expired. Expiration is a separate check.
        } catch (UnsupportedJwtException e) {
            logger.warn("ADMIN-SERVICE: JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("ADMIN-SERVICE: JWT claims string is empty or token is null (during sig validation): {}", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("ADMIN-SERVICE: JWT signature validation FAILED: {}", e.getMessage());
        } catch (Exception e) { // Catch any other potential parsing errors
            logger.warn("ADMIN-SERVICE: Unexpected error during JWT signature validation: {}", e.getMessage());
        }
        return false;
    }
}