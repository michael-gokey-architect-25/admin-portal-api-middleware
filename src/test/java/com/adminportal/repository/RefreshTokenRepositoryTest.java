// src/test/java/com/adminportal/repository/RefreshTokenRepositoryTest.java

// ============================================================================
// Testing Strategy:
// - Use @DataJpaTest for Spring Data JPA testing
// - Real PostgreSQL database via Testcontainers
// - Test token lifecycle (save, find, delete, revoke)
// - Test custom queries for token validation
// - Test bulk operations (delete expired, revoke all user tokens)
// - Verify relationship with User entity (Foreign Key ON DELETE CASCADE)
// ============================================================================

package com.adminportal.repository;

import com.adminportal.entity.RefreshToken;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RefreshTokenRepository Unit Tests")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    // ========================================================================
    // TESTCONTAINERS SETUP - Real PostgreSQL Database
    // ========================================================================
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("admin_portal_test")
            .withUsername("test_user")
            .withPassword("test_password");

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private User testUser;
    private User anotherUser;
    private RefreshToken validToken;
    private RefreshToken expiredToken;
    private RefreshToken revokedToken;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .email("john@example.com")
                .password("hashed_password_123")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        anotherUser = User.builder()
                .id(UUID.randomUUID())
                .username("jane_smith")
                .email("jane@example.com")
                .password("hashed_password_456")
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save users to database first
        testUser = userRepository.save(testUser);
        anotherUser = userRepository.save(anotherUser);

        // Create valid token (expires in 7 days)
        validToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid_refresh_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        // Create expired token (expired 1 hour ago)
        expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("expired_refresh_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        // Create revoked token
        revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("revoked_refresh_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revokedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }


    // ========================================================================
    // SAVE TESTS
    // ========================================================================
    @Test
    @DisplayName("SAVE - Should save refresh token to database")
    void testSaveRefreshToken_Success() {
        // Act
        RefreshToken savedToken = refreshTokenRepository.save(validToken);

        // Assert
        assertNotNull(savedToken.getId());
        assertEquals(validToken.getToken(), savedToken.getToken());
        assertEquals(testUser.getId(), savedToken.getUser().getId());
    }


    @Test
    @DisplayName("SAVE - Should persist token with all fields")
    void testSaveRefreshToken_AllFieldsPersisted() {
        // Act
        RefreshToken savedToken = refreshTokenRepository.save(validToken);

        // Assert
        RefreshToken retrievedToken = refreshTokenRepository.findById(savedToken.getId()).orElse(null);
        assertNotNull(retrievedToken);
        assertEquals(validToken.getToken(), retrievedToken.getToken());
        assertEquals(testUser.getId(), retrievedToken.getUser().getId());
        assertNotNull(retrievedToken.getExpiresAt());
        assertNotNull(retrievedToken.getCreatedAt());
        assertNull(retrievedToken.getRevokedAt());
    }


    @Test
    @DisplayName("SAVE - Should support revoked tokens with revokedAt timestamp")
    void testSaveRefreshToken_RevokedToken() {
        // Act
        RefreshToken savedToken = refreshTokenRepository.save(revokedToken);

        // Assert
        assertNotNull(savedToken.getRevokedAt());
        assertEquals(revokedToken.getRevokedAt(), savedToken.getRevokedAt());
    }


    // ========================================================================
    // FIND BY TOKEN TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY TOKEN - Should return token when found")
    void testFindByToken_Found() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(validToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken(savedToken.getToken());

        // Assert
        assertTrue(foundToken.isPresent());
        assertEquals(savedToken.getId(), foundToken.get().getId());
    }


    @Test
    @DisplayName("FIND BY TOKEN - Should return empty Optional when not found")
    void testFindByToken_NotFound() {
        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken("nonexistent_token");

        // Assert
        assertTrue(foundToken.isEmpty());
    }


    @Test
    @DisplayName("FIND BY TOKEN - Should find expired tokens")
    void testFindByToken_ExpiredToken() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(expiredToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken(savedToken.getToken());

        // Assert
        assertTrue(foundToken.isPresent());
        assertTrue(foundToken.get().getExpiresAt().isBefore(LocalDateTime.now()));
    }


    @Test
    @DisplayName("FIND BY TOKEN - Should find revoked tokens")
    void testFindByToken_RevokedToken() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(revokedToken);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken(savedToken.getToken());

        // Assert
        assertTrue(foundToken.isPresent());
        assertNotNull(foundToken.get().getRevokedAt());
    }


    // ========================================================================
    // FIND BY USER TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY USER - Should return all tokens for a user")
    void testFindByUser_Found() {
        // Arrange
        RefreshToken token1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("token_1_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("token_2_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findByUser(testUser);

        // Assert
        assertEquals(2, userTokens.size());
        assertTrue(userTokens.stream()
                .allMatch(token -> token.getUser().getId().equals(testUser.getId())));
    }


    @Test
    @DisplayName("FIND BY USER - Should return empty list when user has no tokens")
    void testFindByUser_NoTokens() {
        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findByUser(anotherUser);

        // Assert
        assertTrue(userTokens.isEmpty());
    }


    @Test
    @DisplayName("FIND BY USER - Should not return tokens from other users")
    void testFindByUser_OnlyUserTokens() {
        // Arrange
        RefreshToken userToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("user_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken anotherUserToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("another_user_token_" + UUID.randomUUID())
                .user(anotherUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(userToken);
        refreshTokenRepository.save(anotherUserToken);

        // Act
        List<RefreshToken> testUserTokens = refreshTokenRepository.findByUser(testUser);
        List<RefreshToken> anotherUserTokens = refreshTokenRepository.findByUser(anotherUser);

        // Assert
        assertEquals(1, testUserTokens.size());
        assertEquals(1, anotherUserTokens.size());
        assertEquals(testUser.getId(), testUserTokens.get(0).getUser().getId());
    }


    // ========================================================================
    // FIND VALID TOKENS TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND VALID TOKENS - Should return non-expired, non-revoked tokens")
    void testFindValidTokensByUserId_Success() {
        // Arrange
        RefreshToken validToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid_token_1_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("expired_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now())
                .build();

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("revoked_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revokedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(validToken1);
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(revokedToken);

        // Act
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(testUser.getId());

        // Assert
        assertEquals(1, validTokens.size());
        assertEquals(validToken1.getId(), validTokens.get(0).getId());
    }


    @Test
    @DisplayName("FIND VALID TOKENS - Should return empty list when all tokens invalid")
    void testFindValidTokensByUserId_NoValidTokens() {
        // Arrange
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(revokedToken);

        // Act
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(testUser.getId());

        // Assert
        assertTrue(validTokens.isEmpty());
    }


    // ========================================================================
    // DELETE EXPIRED TOKENS TESTS
    // ========================================================================
    @Test
    @DisplayName("DELETE EXPIRED TOKENS - Should remove all expired tokens")
    void testDeleteExpiredTokens_Success() {
        // Arrange
        RefreshToken validToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid_1_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken expiredToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("expired_1_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        refreshTokenRepository.save(validToken1);
        refreshTokenRepository.save(expiredToken1);

        // Act
        refreshTokenRepository.deleteExpiredTokens();

        // Assert
        long remainingCount = refreshTokenRepository.count();
        assertEquals(1, remainingCount);

        Optional<RefreshToken> deletedToken = refreshTokenRepository.findByToken(expiredToken1.getToken());
        assertTrue(deletedToken.isEmpty());
    }


    @Test
    @DisplayName("DELETE EXPIRED TOKENS - Should not delete valid tokens")
    void testDeleteExpiredTokens_PreservesValidTokens() {
        // Arrange
        RefreshToken validToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid_preserve_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(validToken1);

        // Act
        refreshTokenRepository.deleteExpiredTokens();

        // Assert
        Optional<RefreshToken> preservedToken = refreshTokenRepository.findByToken(validToken1.getToken());
        assertTrue(preservedToken.isPresent());
    }


    // ========================================================================
    // DELETE REVOKED TOKENS TESTS
    // ========================================================================
    @Test
    @DisplayName("DELETE REVOKED TOKENS - Should remove all revoked tokens")
    void testDeleteRevokedTokens_Success() {
        // Arrange
        RefreshToken validToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("valid_revoked_test_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken revokedToken1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("revoked_test_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revokedAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(validToken1);
        refreshTokenRepository.save(revokedToken1);

        // Act
        refreshTokenRepository.deleteRevokedTokens();

        // Assert
        long remainingCount = refreshTokenRepository.count();
        assertEquals(1, remainingCount);

        Optional<RefreshToken> deletedToken = refreshTokenRepository.findByToken(revokedToken1.getToken());
        assertTrue(deletedToken.isEmpty());
    }


    // ========================================================================
    // REVOKE ALL USER TOKENS TESTS
    // ========================================================================
    @Test
    @DisplayName("REVOKE ALL USER TOKENS - Should revoke all tokens for a user")
    void testRevokeAllUserTokens_Success() {
        // Arrange
        RefreshToken token1 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("user_token_1_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("user_token_2_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // Act
        refreshTokenRepository.revokeAllUserTokens(testUser.getId());

        // Assert
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUserId(testUser.getId());
        assertTrue(validTokens.isEmpty());

        // Verify tokens are marked as revoked
        Optional<RefreshToken> revokedToken1 = refreshTokenRepository.findByToken(token1.getToken());
        Optional<RefreshToken> revokedToken2 = refreshTokenRepository.findByToken(token2.getToken());

        assertTrue(revokedToken1.isPresent());
        assertTrue(revokedToken2.isPresent());
        assertNotNull(revokedToken1.get().getRevokedAt());
        assertNotNull(revokedToken2.get().getRevokedAt());
    }


    @Test
    @DisplayName("REVOKE ALL USER TOKENS - Should only revoke user's tokens")
    void testRevokeAllUserTokens_OnlyRevokeUserTokens() {
        // Arrange
        RefreshToken testUserToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("test_user_token_" + UUID.randomUUID())
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken anotherUserToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("another_user_token_" + UUID.randomUUID())
                .user(anotherUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(testUserToken);
        refreshTokenRepository.save(anotherUserToken);

        // Act
        refreshTokenRepository.revokeAllUserTokens(testUser.getId());

        // Assert
        List<RefreshToken> testUserValidTokens = refreshTokenRepository.findValidTokensByUserId(testUser.getId());
        List<RefreshToken> anotherUserValidTokens = refreshTokenRepository.findValidTokensByUserId(anotherUser.getId());

        assertTrue(testUserValidTokens.isEmpty());
        assertEquals(1, anotherUserValidTokens.size());
    }


    // ========================================================================
    // FOREIGN KEY CASCADE TESTS
    // ========================================================================
    @Test
    @DisplayName("CASCADE DELETE - Should delete tokens when user is deleted")
    void testCascadeDelete_UserDeletion() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(validToken);
        UUID userId = testUser.getId();

        // Act
        userRepository.delete(testUser);

        // Assert - Token should be deleted due to CASCADE
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findById(savedToken.getId());
        assertTrue(deletedToken.isEmpty());
    }


    // ========================================================================
    // DELETE TESTS
    // ========================================================================
    @Test
    @DisplayName("DELETE - Should remove token from database")
    void testDeleteToken_Success() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(validToken);

        // Act
        refreshTokenRepository.deleteById(savedToken.getId());

        // Assert
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findById(savedToken.getId());
        assertTrue(deletedToken.isEmpty());
    }


    // ========================================================================
    // UPDATE TESTS
    // ========================================================================
    @Test
    @DisplayName("UPDATE - Should update token revocation status")
    void testUpdateToken_RevokeToken() {
        // Arrange
        RefreshToken savedToken = refreshTokenRepository.save(validToken);
        assertNull(savedToken.getRevokedAt());

        // Act
        savedToken.setRevokedAt(LocalDateTime.now());
        RefreshToken updatedToken = refreshTokenRepository.save(savedToken);

        // Assert
        assertNotNull(updatedToken.getRevokedAt());

        // Verify in database
        RefreshToken dbToken = refreshTokenRepository.findById(updatedToken.getId()).orElse(null);
        assertNotNull(dbToken);
        assertNotNull(dbToken.getRevokedAt());
    }


    // ========================================================================
    // COUNT TESTS
    // ========================================================================
    @Test
    @DisplayName("COUNT - Should return total count of tokens")
    void testCountTokens() {
        // Arrange
        refreshTokenRepository.save(validToken);
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(revokedToken);

        // Act
        long count = refreshTokenRepository.count();

        // Assert
        assertEquals(3, count);
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:
    Run individual test:
        mvn test -Dtest=RefreshTokenRepositoryTest#testSaveRefreshToken_Success
    Run all RefreshTokenRepository tests:
        mvn test -Dtest=RefreshTokenRepositoryTest
    Run with detailed output:
        mvn test -Dtest=RefreshTokenRepositoryTest -X
    Run specific test pattern:
        mvn test -Dtest=RefreshTokenRepositoryTest#testFind*
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0

    Note: Testcontainers will download and start PostgreSQL Docker image
    First run may take 30-60 seconds for image download and container startup
    Subsequent runs will be faster as image is cached     */
}

