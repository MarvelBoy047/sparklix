package com.sparklix.userservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${sparklix.app.jwtSecret}") // CORRECTED
    private String jwtSecretString;

    @Value("${sparklix.app.jwtExpirationMs}") // CORRECTED
    private int jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        if (jwtSecretString == null || jwtSecretString.getBytes().length < 32) {
            logger.warn("JWT Secret key is not defined or too short in properties for user-service. Using a default secure key for development (NOT FOR PRODUCTION).");
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            byte[] keyBytes = jwtSecretString.getBytes();
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String roles = userPrincipal.getAuthorities().stream()
                           .map(GrantedAuthority::getAuthority)
                           .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public Claims getAllClaimsFromToken(String token) {
         try {
            return Jwts.parserBuilder()
                       .setSigningKey(key)
                       .build()
                       .parseClaimsJws(token)
                       .getBody();
        } catch (ExpiredJwtException e) {
            logger.trace("JWT token is expired during claim extraction: {}", e.getMessage());
            return e.getClaims();
        } catch (JwtException e) {
            logger.warn("Could not parse JWT claims for token '{}': {}", token, e.getMessage());
            return null;
        }
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        try {
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.trace("Could not apply claims resolver, possibly due to expired token structure: {}", e.getMessage());
            return null;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) return true;
            return expiration.before(new Date());
        } catch(ExpiredJwtException e){
            return true;
        }
    }

    public Boolean validateTokenSignature(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token (structure): {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired (checked during signature validation attempt): {}", e.getMessage());
            return true;
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty or token is null (during signature validation): {}", e.getMessage());
        } catch (SignatureException e) {
            logger.warn("JWT signature validation failed: {}", e.getMessage());
        }
        return false;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("UserDetails is null, cannot validate token further.");
            return false;
        }
        final String username = getUsernameFromToken(token);
        if (username == null) {
             logger.warn("Username could not be extracted from token for validation.");
            return false;
        }
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}