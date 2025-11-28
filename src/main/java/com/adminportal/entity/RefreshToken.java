// admin-portal-api/src/main/java/com/adminportal/entity/RefreshToken.java

// ============================================================================
// PURPOSE: JPA Entity for storing refresh tokens
// - Enables long-lived user sessions without re-authentication
// - Separate table allows invalidating tokens independently
// - Tracks token expiration for cleanup
// KEY CONCEPTS:
// 1. Many-to-One: Multiple tokens per user (one per device/session)
// 2. Token Revocation: Can invalidate tokens server-side
// 3. Expiration Cleanup: Database should periodically delete expired tokens
// ============================================================================

package com.adminportal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken Entity - Manages JWT refresh tokens
 * DATABASE TABLE: refresh_tokens
 * RELATIONSHIPS: Many-to-One with User
 * PURPOSE: Enable token refresh without user re-authentication
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_token", columnList = "token"),
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  
    /** ==================== PRIMARY KEY ====================
     * Unique identifier for refresh token record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;


    
    /** ==================== FOREIGN KEY ====================
     * Reference to User entity
     * ManyToOne: Multiple tokens can exist for one user
     * FetchType.LAZY: Load user only when accessed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKeyName = "fk_refresh_tokens_user_id"
    )
    private User user;

    

    /** ==================== TOKEN FIELD ====================
     * Actual JWT refresh token string
     * UNIQUE: Only one active token per session
     * Typically 256 characters (base64 encoded)
     */
    @Column(name = "token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;


    /** ==================== EXPIRATION ====================
     * When this refresh token expires
     * Typically 7 days from creation
     * After expiration, user must login again
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;


    
    /**  ==================== AUDIT ====================
     * When token was created (issued)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When token was revoked (optional)
     * Null if token not yet revoked
     * Set when user logs out
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;


    /** ==================== HELPER METHODS ====================
     * Check if token has expired
     * @return true if current time is after expiresAt
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token has been revoked
     * @return true if revokedAt is not null
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Check if token is still valid
     * Valid = not expired AND not revoked
     * @return true if token can still be used
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}



