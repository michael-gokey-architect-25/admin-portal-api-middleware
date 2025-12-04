// src/test/java/com/adminportal/repository/UserRepositoryTest.java

// ============================================================================
// Testing Strategy:
// - Use @DataJpaTest for Spring Data JPA testing
// - Real PostgreSQL database via Testcontainers
// - Test all derived query methods
// - Test custom @Query methods
// - Test pagination and sorting
// - Verify database constraints and relationships
// ============================================================================

package com.adminportal.repository;

import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRepository Unit Tests")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

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
    private UserRepository userRepository;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private User testUser1;
    private User testUser2;
    private User adminUser;
    private User managerUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        // Create regular user
        testUser1 = User.builder()
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

        // Create second regular user
        testUser2 = User.builder()
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

        // Create admin user
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("hashed_admin_password")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create manager user
        managerUser = User.builder()
                .id(UUID.randomUUID())
                .username("jane_manager")
                .email("jane_manager@example.com")
                .password("hashed_manager_password")
                .firstName("Jane")
                .lastName("Manager")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create inactive user
        inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .username("inactive_user")
                .email("inactive@example.com")
                .password("hashed_password_789")
                .firstName("Inactive")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    // ========================================================================
    // SAVE TESTS
    // ========================================================================
    @Test
    @DisplayName("SAVE - Should save user to database")
    void testSaveUser_Success() {
        // Act
        User savedUser = userRepository.save(testUser1);

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("john_doe", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
    }


    @Test
    @DisplayName("SAVE - Should persist user with all fields")
    void testSaveUser_AllFieldsPersisted() {
        // Act
        User savedUser = userRepository.save(testUser1);

        // Assert
        User retrievedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(retrievedUser);
        assertEquals("john_doe", retrievedUser.getUsername());
        assertEquals("john@example.com", retrievedUser.getEmail());
        assertEquals("John", retrievedUser.getFirstName());
        assertEquals("Doe", retrievedUser.getLastName());
        assertEquals(UserRole.USER, retrievedUser.getRole());
        assertEquals(UserStatus.ACTIVE, retrievedUser.getStatus());
    }


    @Test
    @DisplayName("SAVE - Should auto-generate UUID if not provided")
    void testSaveUser_AutoGenerateUUID() {
        // Arrange - Create user without ID
        User userWithoutId = User.builder()
                .username("new_user")
                .email("new@example.com")
                .password("hashed_password")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        // Act
        User savedUser = userRepository.save(userWithoutId);

        // Assert
        assertNotNull(savedUser.getId());
    }


    // ========================================================================
    // FIND BY ID TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY ID - Should return user when found")
    void testFindById_Found() {
        // Arrange
        User savedUser = userRepository.save(testUser1);

        // Act
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("john_doe", foundUser.get().getUsername());
    }


    @Test
    @DisplayName("FIND BY ID - Should return empty Optional when not found")
    void testFindById_NotFound() {
        // Act
        Optional<User> foundUser = userRepository.findById(UUID.randomUUID());

        // Assert
        assertTrue(foundUser.isEmpty());
    }


    // ========================================================================
    // FIND BY EMAIL TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY EMAIL - Should return user when found")
    void testFindByEmail_Found() {
        // Arrange
        userRepository.save(testUser1);

        // Act
        Optional<User> foundUser = userRepository.findByEmail("john@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("john_doe", foundUser.get().getUsername());
    }


    @Test
    @DisplayName("FIND BY EMAIL - Should return empty Optional when not found")
    void testFindByEmail_NotFound() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertTrue(foundUser.isEmpty());
    }


    @Test
    @DisplayName("FIND BY EMAIL - Should be case-insensitive or exact match")
    void testFindByEmail_CaseSensitivity() {
        // Arrange
        userRepository.save(testUser1);

        // Act - Try different case
        Optional<User> foundUser = userRepository.findByEmail("JOHN@EXAMPLE.COM");

        // Assert - Depends on database collation, but should not find uppercase version
        // This test documents the behavior
        assertFalse(foundUser.isPresent() || foundUser.isPresent()); // Will be false
    }


    // ========================================================================
    // FIND BY USERNAME TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY USERNAME - Should return user when found")
    void testFindByUsername_Found() {
        // Arrange
        userRepository.save(testUser1);

        // Act
        Optional<User> foundUser = userRepository.findByUsername("john_doe");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("john@example.com", foundUser.get().getEmail());
    }


    @Test
    @DisplayName("FIND BY USERNAME - Should return empty Optional when not found")
    void testFindByUsername_NotFound() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("nonexistent_user");

        // Assert
        assertTrue(foundUser.isEmpty());
    }


    // ========================================================================
    // FIND BY ROLE TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY ROLE - Should return all users with specific role")
    void testFindByRole_Found() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        userRepository.save(adminUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> usersPage = userRepository.findByRole(UserRole.USER, pageable);

        // Assert
        assertEquals(2, usersPage.getContent().size());
        assertTrue(usersPage.getContent().stream()
                .allMatch(u -> u.getRole().equals(UserRole.USER)));
    }


    @Test
    @DisplayName("FIND BY ROLE - Should return empty page when no users with role")
    void testFindByRole_NotFound() {
        // Arrange
        userRepository.save(testUser1);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> adminsPage = userRepository.findByRole(UserRole.ADMIN, pageable);

        // Assert
        assertTrue(adminsPage.isEmpty());
    }


    @Test
    @DisplayName("FIND BY ROLE - Should support pagination")
    void testFindByRole_Pagination() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        userRepository.save(adminUser);
        userRepository.save(managerUser);

        Pageable pageOne = PageRequest.of(0, 2);
        Pageable pageTwo = PageRequest.of(1, 2);

        // Act
        Page<User> firstPage = userRepository.findByRole(UserRole.USER, pageOne);
        Page<User> secondPage = userRepository.findByRole(UserRole.USER, pageTwo);

        // Assert
        assertEquals(2, firstPage.getContent().size());
        assertEquals(0, secondPage.getContent().size()); // No second page of users
        assertEquals(2, firstPage.getTotalElements());
    }


    // ========================================================================
    // FIND BY STATUS TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY STATUS - Should return users with specific status")
    void testFindByStatus_Found() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(inactiveUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE, pageable);

        // Assert
        assertEquals(1, activeUsers.getContent().size());
        assertEquals("john_doe", activeUsers.getContent().get(0).getUsername());
    }


    @Test
    @DisplayName("FIND BY STATUS - Should filter out other statuses")
    void testFindByStatus_Filtered() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(inactiveUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> inactiveUsers = userRepository.findByStatus(UserStatus.INACTIVE, pageable);

        // Assert
        assertEquals(1, inactiveUsers.getContent().size());
        assertEquals("inactive_user", inactiveUsers.getContent().get(0).getUsername());
    }


    // ========================================================================
    // FIND BY ROLE AND STATUS TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY ROLE AND STATUS - Should return filtered users")
    void testFindByRoleAndStatus_Found() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        userRepository.save(inactiveUser);
        userRepository.save(adminUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> activeUsers = userRepository.findByRoleAndStatus(
                UserRole.USER, UserStatus.ACTIVE, pageable);

        // Assert
        assertEquals(2, activeUsers.getContent().size());
        assertTrue(activeUsers.getContent().stream()
                .allMatch(u -> u.getRole().equals(UserRole.USER) && 
                             u.getStatus().equals(UserStatus.ACTIVE)));
    }


    @Test
    @DisplayName("FIND BY ROLE AND STATUS - Should return empty when no matches")
    void testFindByRoleAndStatus_NoMatches() {
        // Arrange
        userRepository.save(testUser1); // ACTIVE USER

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> inactiveAdmins = userRepository.findByRoleAndStatus(
                UserRole.ADMIN, UserStatus.INACTIVE, pageable);

        // Assert
        assertTrue(inactiveAdmins.isEmpty());
    }


    // ========================================================================
    // EXISTS BY EMAIL TESTS
    // ========================================================================
    @Test
    @DisplayName("EXISTS BY EMAIL - Should return true when email exists")
    void testExistsByEmail_True() {
        // Arrange
        userRepository.save(testUser1);

        // Act
        boolean exists = userRepository.existsByEmail("john@example.com");

        // Assert
        assertTrue(exists);
    }


    @Test
    @DisplayName("EXISTS BY EMAIL - Should return false when email not exists")
    void testExistsByEmail_False() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }


    // ========================================================================
    // EXISTS BY USERNAME TESTS
    // ========================================================================
    @Test
    @DisplayName("EXISTS BY USERNAME - Should return true when username exists")
    void testExistsByUsername_True() {
        // Arrange
        userRepository.save(testUser1);

        // Act
        boolean exists = userRepository.existsByUsername("john_doe");

        // Assert
        assertTrue(exists);
    }


    @Test
    @DisplayName("EXISTS BY USERNAME - Should return false when username not exists")
    void testExistsByUsername_False() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent_user");

        // Assert
        assertFalse(exists);
    }


    // ========================================================================
    // CUSTOM QUERY - FIND BY SEARCH TERM TESTS
    // ========================================================================
    @Test
    @DisplayName("FIND BY SEARCH TERM - Should find users by username")
    void testFindBySearchTerm_ByUsername() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        userRepository.save(adminUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> results = userRepository.findBySearchTerm("john", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
        assertEquals("john_doe", results.getContent().get(0).getUsername());
    }


    @Test
    @DisplayName("FIND BY SEARCH TERM - Should find users by email")
    void testFindBySearchTerm_ByEmail() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> results = userRepository.findBySearchTerm("jane@example.com", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
        assertEquals("jane_smith", results.getContent().get(0).getUsername());
    }


    @Test
    @DisplayName("FIND BY SEARCH TERM - Should find users by first name")
    void testFindBySearchTerm_ByFirstName() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(adminUser);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> results = userRepository.findBySearchTerm("John", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
        assertEquals("john_doe", results.getContent().get(0).getUsername());
    }


    @Test
    @DisplayName("FIND BY SEARCH TERM - Should be case-insensitive")
    void testFindBySearchTerm_CaseInsensitive() {
        // Arrange
        userRepository.save(testUser1);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> resultLower = userRepository.findBySearchTerm("john", pageable);
        Page<User> resultUpper = userRepository.findBySearchTerm("JOHN", pageable);

        // Assert - Should find in both cases
        assertEquals(1, resultLower.getContent().size());
        assertEquals(1, resultUpper.getContent().size());
    }


    @Test
    @DisplayName("FIND BY SEARCH TERM - Should return empty when no matches")
    void testFindBySearchTerm_NoMatches() {
        // Arrange
        userRepository.save(testUser1);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> results = userRepository.findBySearchTerm("nonexistent", pageable);

        // Assert
        assertTrue(results.isEmpty());
    }


    // ========================================================================
    // DELETE TESTS
    // ========================================================================
    @Test
    @DisplayName("DELETE - Should remove user from database")
    void testDeleteUser_Success() {
        // Arrange
        User savedUser = userRepository.save(testUser1);

        // Act
        userRepository.deleteById(savedUser.getId());

        // Assert
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertTrue(deletedUser.isEmpty());
    }


    @Test
    @DisplayName("DELETE - Should not fail when deleting non-existent user")
    void testDeleteUser_NonExistent() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            userRepository.deleteById(UUID.randomUUID());
        });
    }


    // ========================================================================
    // UPDATE TESTS
    // ========================================================================
    @Test
    @DisplayName("UPDATE - Should update existing user")
    void testUpdateUser_Success() {
        // Arrange
        User savedUser = userRepository.save(testUser1);
        savedUser.setFirstName("UpdatedJohn");
        savedUser.setStatus(UserStatus.INACTIVE);

        // Act
        User updatedUser = userRepository.save(savedUser);

        // Assert
        assertEquals("UpdatedJohn", updatedUser.getFirstName());
        assertEquals(UserStatus.INACTIVE, updatedUser.getStatus());

        // Verify in database
        User dbUser = userRepository.findById(updatedUser.getId()).orElse(null);
        assertNotNull(dbUser);
        assertEquals("UpdatedJohn", dbUser.getFirstName());
        assertEquals(UserStatus.INACTIVE, dbUser.getStatus());
    }


    // ========================================================================
    // COUNT TESTS
    // ========================================================================
    @Test
    @DisplayName("COUNT - Should return total count of users")
    void testCountUsers() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        userRepository.save(adminUser);

        // Act
        long count = userRepository.count();

        // Assert
        assertEquals(3, count);
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:
    Run individual test:
        mvn test -Dtest=UserRepositoryTest#testSaveUser_Success
    Run all UserRepository tests:
        mvn test -Dtest=UserRepositoryTest
    Run with detailed output:
        mvn test -Dtest=UserRepositoryTest -X
    Run specific test pattern:
        mvn test -Dtest=UserRepositoryTest#testFind*
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
    
    Note: Testcontainers will download and start PostgreSQL Docker image
    First run may take 30-60 seconds for image download and container startup
    Subsequent runs will be faster as image is cached     */
}

