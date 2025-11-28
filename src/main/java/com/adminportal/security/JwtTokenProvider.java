
// admin-portal-api/src/main/java/com/adminportal/security/JwtTokenProvider.java


// ============================================================================
// PURPOSE: JWT (JSON Web Token) creation, validation, and parsing
// - Generates access and refresh tokens
// - Validates token signatures and expiration
// - Extracts claims (user info) from tokens
// - Configurable token expiration times
// KEY CONCEPTS:
// 1. JWT Structure: header.payload.signature (Base64 encoded)
// 2. Claims: Data embedded in token (userId, roles, exp)
// 3. Signature: Prevents token tampering using secret key
// 4. Expiration: Token becomes invalid after specified time
// 5. Algorithms: HS256 (HMAC) simpler, RS256 (RSA) more secure
// TEACHING NOTES:
// - Never expose JWT secret in logs or error messages
// - Use configurable expiration (60 min access, 7 days refresh)
// - Always validate token signature before trusting claims
// - Token should be stored in HttpOnly cookie (if possible)
// ============================================================================

package com.adminportal.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * JWT Token Provider - Handles JWT token generation and validation
 * CONFIGURATION:
 * - jwt.secret: Secret key for signing tokens (load from environment)
 * - jwt.expiration: Access token expiration in milliseconds (60 minutes)
 * - jwt.refresh-expiration: Refresh token expiration (7 days)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    // Configuration values injected from application.yml
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}") // Default 60 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // Default 7 days
    private long refreshTokenExpiration;

    private SecretKey secretKey;


    
    /** ==================== INITIALIZATION ====================
     * Post-construct method - runs after constructor
     * Converts secret string into cryptographic key
     * Called automatically by Spring
     */
    @PostConstruct
    public void init() {
        // Keys.hmacShaKeyFor requires at least 32 characters for HS256
        // Never log the actual secret key
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("JWT Token Provider initialized");
    }


    
    /** ==================== TOKEN GENERATION ====================
     * Generate access token (short-lived, 60 minutes)
     * @param authentication Spring Security authentication object
     * @return JWT access token string
     */
    public String generateAccessToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "ACCESS");
        claims.put("roles", authentication.getAuthorities());

        return buildToken(claims, username, accessTokenExpiration);
    }

    /**
     * Generate access token from user ID
     * Used when issuing new token from refresh token
     * @param userId User UUID
     * @param username User username
     * @param roles User roles as comma-separated string
     * @return JWT access token
     */
    public String generateAccessTokenFromUserId(UUID userId, String username, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "ACCESS");
        claims.put("userId", userId.toString());
        claims.put("roles", roles);

        return buildToken(claims, username, accessTokenExpiration);
    }

    /**
     * Generate refresh token (long-lived, 7 days)
     * Used for getting new access tokens without re-login
     * @param userId User UUID
     * @param username User username
     * @return JWT refresh token string
     */
    public String generateRefreshToken(UUID userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        claims.put("userId", userId.toString());

        return buildToken(claims, username, refreshTokenExpiration);
    }

    /**
     * Core token building method
     * Creates JWT with specified claims and expiration
     * @param claims Custom claims to embed in token
     * @param subject Token subject (usually username)
     * @param expirationTime Token lifetime in milliseconds
     * @return Signed JWT token string
     */
    private String buildToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
            .claims(claims)                          // Set custom claims
            .subject(subject)                        // Set token subject (username)
            .issuedAt(now)                          // Set issue time (iat claim)
            .expiration(expiryDate)                 // Set expiration time (exp claim)
            .signWith(secretKey, SignatureAlgorithm.HS256)  // Sign with secret
            .compact();                              // Serialize to string
    }

    // 
    /** ==================== TOKEN VALIDATION ====================
     * Validate token signature and expiration
     * Should be called before using token claims
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyingKey(secretKey)            // Use secret key to verify signature
                .build()
                .parseSignedClaims(token);           // Parse and validate

            log.debug("JWT Token validated successfully");
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired without throwing exception
     * @param token JWT token string
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    
    /** ==================== CLAIM EXTRACTION ====================
     * Extract username from token
     * Token subject is typically the username
     * @param token JWT token string
     * @return Username extracted from token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Extract user ID (UUID) from token claims
     *
     * @param token JWT token string
     * @return User ID UUID as string
     */
    public String getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extract token type (ACCESS or REFRESH)
     *
     * @param token JWT token string
     * @return Token type
     */
    public String getTokenTypeFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extract expiration date from token
     *
     * @param token JWT token string
     * @return Token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from token
     * Uses functional interface for flexibility
     *
     * @param token JWT token string
     * @param claimsResolver Function to extract specific claim
     * @param <T> Type of claim value
     * @return Extracted claim value
     */
    public <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and return all claims from token
     * Internal method - called by other extraction methods
     *
     * @param token JWT token string
     * @return All claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyingKey(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

// public class JwtTokenProvider {
    
// }
