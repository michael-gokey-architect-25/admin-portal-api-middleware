// admin-portal-api/src/main/java/com/adminportal/repository/RefreshTokenRepository.java


// ============================================================================
// PURPOSE: Spring Data JPA repository for RefreshToken entity
// - Manage JWT refresh token records
// - Find tokens by various criteria
// - Support token lifecycle management
// ============================================================================

package com.adminportal.repository;

import com.adminportal.entity.RefreshToken;
import com.adminportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token Repository
 * RESPONSIBILITIES:
 * - Store refresh tokens securely
 * - Query tokens for validation
 * - Clean up expired/revoked tokens
 * - Manage user sessions
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // ==================== FINDING TOKENS ====================
    /**
     * Find refresh token by token string
     * Used during token refresh and logout
     *
     * @param token JWT refresh token string
     * @return Optional containing token if found
     */
    Optional<RefreshToken> findByToken(String token);


    /**
     * Find all tokens for a specific user
     * Used to logout from all devices
     *
     * @param user User entity
     * @return List of all user's tokens
     */
    List<RefreshToken> findByUser(User user);



    // ==================== TOKEN LIFECYCLE ====================
    /**
     * Find valid tokens for user (not expired, not revoked)
     * Used for active session management
     *
     * SQL:
     * SELECT * FROM refresh_tokens WHERE
     *   user_id = ? AND
     *   expires_at > NOW() AND
     *   revoked_at IS NULL
     *
     * @param userId User ID
     * @return List of valid tokens
     */
    @Query(
        "SELECT rt FROM RefreshToken rt WHERE " +
        "rt.user.id = :userId AND " +
        "rt.expiresAt > CURRENT_TIMESTAMP AND " +
        "rt.revokedAt IS NULL"
    )
    List<RefreshToken> findValidTokensByUserId(@Param("userId") UUID userId);


    /**
     * Delete expired tokens (maintenance query)
     * Deletes tokens where expiration date has passed
     *
     * SQL:
     * DELETE FROM refresh_tokens WHERE expires_at < NOW()
     *
     * @return Number of deleted tokens
     */
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    int deleteExpiredTokens();


    /**
     * Delete revoked tokens (maintenance query)
     * Deletes tokens that have been explicitly revoked
     *
     * SQL:
     * DELETE FROM refresh_tokens WHERE revoked_at IS NOT NULL
     *
     * @return Number of deleted tokens
     */
    @Query("DELETE FROM RefreshToken rt WHERE rt.revokedAt IS NOT NULL")
    int deleteRevokedTokens();


    /**
     * Revoke all user tokens (logout from all devices)
     * Sets revoked_at timestamp for all user's tokens
     *
     * @param userId User ID
     * @param now Current timestamp
     * @return Number of tokens revoked
     */
    @Query(
        "UPDATE RefreshToken rt SET rt.revokedAt = :now " +
        "WHERE rt.user.id = :userId AND rt.revokedAt IS NULL"
    )
    int revokeAllUserTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}



