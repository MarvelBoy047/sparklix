package com.sparklix.showcatalogservice.util; // Correct package

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

    @Value("${sparklix.app.jwtSecret}") // Make sure this property exists in catalog's application.properties
    private String jwtSecretString;

    // jwtExpirationMs is not strictly needed for validation-only util, but if copied, ensure property exists or remove field
    // @Value("${sparklix.app.jwtExpirationMs}")
    // private int jwtExpirationMs; 

    private Key key;

    @PostConstruct
    public void init() {
        if (jwtSecretString == null || jwtSecretString.getBytes().length < 32) {
            logger.error("SHOW-CATALOG-SERVICE CRITICAL: JWT Secret key is not defined or too short!");
            // Fallback for startup, but validation will likely fail if secret isn't correctly configured
            this.key = Keys.hmacShaKeyFor("FallbackSecretKeyForShowCatalogService32BytesLong!".getBytes());
        } else {
            byte[] keyBytes = jwtSecretString.getBytes();
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("SHOW-CATALOG-SERVICE: JWT Secret Key initialized successfully.");
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) {
            logger.debug("SHOW-CATALOG-SERVICE: Cannot get roles, claims are null for token.");
            return new ArrayList<>();
        }
        String rolesString = claims.get("roles", String.class);
        if (rolesString != null && !rolesString.isEmpty()) {
            return Arrays.asList(rolesString.split(","));
        }
        logger.debug("SHOW-CATALOG-SERVICE: No 'roles' claim found or is empty in token.");
        return new ArrayList<>();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        if (claims == null) return null;
        try {
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.trace("SHOW-CATALOG-SERVICE: Could not apply claims resolver: {}", e.getMessage());
            return null;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                       .setSigningKey(key)
                       .build()
                       .parseClaimsJws(token)
                       .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("SHOW-CATALOG-SERVICE: JWT token is expired when extracting claims: {}", e.getMessage());
            return e.getClaims(); 
        } catch (JwtException e) { 
            logger.warn("SHOW-CATALOG-SERVICE: Could not parse JWT claims: {}. Token: {}", e.getMessage(), token);
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            if (claims == null) return true; // If claims can't be parsed, treat as expired or invalid
            final Date expiration = claims.getExpiration();
            if (expiration == null) {
                logger.warn("SHOW-CATALOG-SERVICE: Token has no expiration claim.");
                return true; 
            }
            return expiration.before(new Date());
        } catch (Exception e) { // Catch broader exceptions during claim parsing for isTokenExpired
             logger.error("SHOW-CATALOG-SERVICE: Error during isTokenExpired check", e);
             return true;
        }
    }

    public boolean validateTokenSignature(String token) {
        if (token == null) return false;
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            logger.debug("SHOW-CATALOG-SERVICE: JWT signature is valid.");
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("SHOW-CATALOG-SERVICE: Invalid JWT token (Malformed): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("SHOW-CATALOG-SERVICE: JWT token is EXPIRED (checked during sig validation): {}", e.getMessage());
            // An expired token can still have a valid signature. The caller should check isTokenExpired separately if needed.
            return true; 
        } catch (UnsupportedJwtException e) {
            logger.warn("SHOW-CATALOG-SERVICE: JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("SHOW-CATALOG-SERVICE: JWT claims string is empty or token is null: {}", e.getMessage());
        } catch (SignatureException e) { 
            logger.warn("SHOW-CATALOG-SERVICE: JWT signature validation FAILED: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("SHOW-CATALOG-SERVICE: Unexpected error during JWT signature validation: {}", e.getMessage());
        }
        return false;
    }
}