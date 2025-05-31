package com.sparklix.bookingservice.util;

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
            logger.error("BOOKING-SERVICE CRITICAL: JWT Secret key is not defined or too short!");
            // Fallback for startup, but validation will fail if secret isn't correctly configured to match issuer.
            this.key = Keys.hmacShaKeyFor("FallbackBookingSecretKey32BytesLong!DoNotUse!".getBytes());
        } else {
            byte[] keyBytes = jwtSecretString.getBytes();
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("BOOKING-SERVICE: JWT Secret Key initialized successfully for token validation.");
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims == null) { 
            logger.debug("BOOKING-SERVICE: Cannot get roles, claims are null for token.");
            return new ArrayList<>(); 
        }
        String rolesString = claims.get("roles", String.class);
        if (rolesString != null && !rolesString.isEmpty()) {
            return Arrays.asList(rolesString.split(","));
        }
        logger.debug("BOOKING-SERVICE: No 'roles' claim found or claim is empty in token.");
        return new ArrayList<>();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        if (claims == null) return null;
        try { return claimsResolver.apply(claims); } 
        catch (Exception e) { logger.trace("BOOKING-SERVICE: Could not apply claims resolver: {}", e.getMessage()); return null; }
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("BOOKING-SERVICE: JWT token is EXPIRED when extracting claims: {}", e.getMessage());
            return e.getClaims(); 
        } catch (JwtException e) { 
            logger.warn("BOOKING-SERVICE: Could not parse JWT claims for validation: {}. Token: {}", e.getMessage(), token != null ? token.substring(0, Math.min(token.length(),15))+"..." : "null");
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            if (claims == null) return true; // Cannot determine if unparseable
            final Date expiration = claims.getExpiration();
            if (expiration == null) { logger.warn("BOOKING-SERVICE: Token has no expiration claim."); return true; }
            boolean expired = expiration.before(new Date());
            if (expired) logger.warn("BOOKING-SERVICE: Token confirmed expired. Expiration: {}, Current: {}", expiration, new Date());
            return expired;
        } catch (Exception e) { logger.error("BOOKING-SERVICE: Error during isTokenExpired check", e); return true; }
    }

    public boolean validateTokenSignature(String token) {
        if (token == null) return false;
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            logger.debug("BOOKING-SERVICE: JWT signature is valid.");
            return true;
        } catch (MalformedJwtException e) { logger.warn("BOOKING-SERVICE: Invalid JWT token (Malformed): {}", e.getMessage());
        } catch (ExpiredJwtException e) { logger.warn("BOOKING-SERVICE: JWT token is EXPIRED (but signature ok): {}", e.getMessage()); return true;
        } catch (UnsupportedJwtException e) { logger.warn("BOOKING-SERVICE: JWT token unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) { logger.warn("BOOKING-SERVICE: JWT claims empty/null: {}", e.getMessage());
        } catch (SignatureException e) { logger.warn("BOOKING-SERVICE: JWT signature FAILED: {}", e.getMessage());
        } catch (Exception e) { logger.warn("BOOKING-SERVICE: Unexpected JWT signature validation error: {}", e.getMessage());}
        return false;
    }
}