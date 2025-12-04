// src/test/java/com/adminportal/integration/UserIntegrationTest.java

// ============================================================================
// Testing Strategy:
// - Use @SpringBootTest for full application context
// - Real database (Testcontainers PostgreSQL)
// - Test complete user management workflows
// - Real HTTP requests via TestRestTemplate
// - Test CRUD operations, search, pagination, bulk operations
// - Verify authorization (admin vs regular user)
// ============================================================================

package com.adminportal.integration;

import com.adminportal.dto.request.BulkDeleteRequest;
import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.response.BulkDeleteResponse;
import com.adminportal.dto.response.LoginResponse;
import com.adminportal.dto.response.UserListResponse;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class UserIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private User adminUser;
    private User regularUser;
    private String adminAccessToken;
    private String userAccessToken;

    @BeforeEach
    void setUp() {
        // Clear database
        userRepository.deleteAll();

        // Create admin user (hashed password for "AdminPassword123!")
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("$2a$10$slYQmyNdGzin7olVN7q2OPST9/PgBkqquzi8Ay0IQi1VG5e.CCcga")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create regular user (hashed password for "UserPassword123!")
        regularUser = User.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .email("john@example.com")
                .password("$2a$10$slYQmyNdGzin7olVN7q2OPST9/PgBkqquzi8Ay0IQi1VG5e.CCcga")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save users to database
        userRepository.save(adminUser);
        userRepository.save(regularUser);

        // Get admin access token
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@example.com")
                .password("TestPassword123!")
                .build();

        ResponseEntity<LoginResponse> adminLoginResponse = restTemplate.postForEntity(
                "/auth/login",
                adminLogin,
                LoginResponse.class);

        adminAccessToken = adminLoginResponse.getBody().getAccessToken();

        // Get regular user access token
        LoginRequest userLogin = LoginRequest.builder()
                .email("john@example.com")
                .password("TestPassword123!")
                .build();

        ResponseEntity<LoginResponse> userLoginResponse = restTemplate.postForEntity(
                "/auth/login",
                userLogin,
                LoginResponse.class);

        userAccessToken = userLoginResponse.getBody().getAccessToken();
    }


    // ========================================================================
    // WORKFLOW TEST 1: CREATE → READ → UPDATE → DELETE
    // ========================================================================
    @Test
    @DisplayName("COMPLETE CRUD FLOW - Admin Creates, Reads, Updates, Deletes User")
    void testCompleteCRUDWorkflow() {
        // STEP 1: Create new user (admin only)
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username("new_user")
                .email("new@example.com")
                .password("NewPassword123!")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                "/users",
                new HttpEntity<>(createRequest, adminHeaders),
                UserResponse.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        UUID newUserId = createResponse.getBody().getId();
        assertEquals("new_user", createResponse.getBody().getUsername());

        // STEP 2: Read user (any authenticated user)
        ResponseEntity<UserResponse> readResponse = restTemplate.exchange(
                "/users/" + newUserId,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                UserResponse.class);

        assertEquals(HttpStatus.OK, readResponse.getStatusCode());
        assertEquals("new_user", readResponse.getBody().getUsername());

        // STEP 3: Update user (admin only)
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("NewUser")
                .status(UserStatus.INACTIVE)
                .build();

        ResponseEntity<UserResponse> updateResponse = restTemplate.exchange(
                "/users/" + newUserId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders),
                UserResponse.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated", updateResponse.getBody().getFirstName());
        assertEquals(UserStatus.INACTIVE, updateResponse.getBody().getStatus());

        // STEP 4: Delete user (admin only)
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/users/" + newUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders),
                Void.class);

        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        // STEP 5: Verify user is deleted
        ResponseEntity<String> verifyDeleteResponse = restTemplate.exchange(
                "/users/" + newUserId,
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                String.class);

        assertEquals(HttpStatus.NOT_FOUND, verifyDeleteResponse.getStatusCode());
    }


    // ========================================================================
    // WORKFLOW TEST 2: SEARCH USERS
    // ========================================================================
    @Test
    @DisplayName("SEARCH FLOW - Admin Searches for Users")
    void testSearchUsersWorkflow() {
        // Arrange - Create multiple users
        for (int i = 0; i < 3; i++) {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("user_" + i)
                    .email("user" + i + "@example.com")
                    .password("hashed_password")
                    .firstName("User" + i)
                    .lastName("Test")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act - Search by username
        ResponseEntity<UserListResponse> searchResponse = restTemplate.exchange(
                "/users/search?term=user_1",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                UserListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertNotNull(searchResponse.getBody());
        assertEquals(1, searchResponse.getBody().getUsers().size());
        assertEquals("user_1", searchResponse.getBody().getUsers().get(0).getUsername());
    }


    // ========================================================================
    // WORKFLOW TEST 3: PAGINATION AND FILTERING
    // ========================================================================
    @Test
    @DisplayName("PAGINATION FLOW - Admin Filters and Paginates Users")
    void testPaginationAndFilteringWorkflow() {
        // Arrange - Create users with different roles
        User manager1 = User.builder()
                .id(UUID.randomUUID())
                .username("manager1")
                .email("manager1@example.com")
                .password("hashed_password")
                .firstName("Manager")
                .lastName("One")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build();

        User manager2 = User.builder()
                .id(UUID.randomUUID())
                .username("manager2")
                .email("manager2@example.com")
                .password("hashed_password")
                .firstName("Manager")
                .lastName("Two")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(manager1);
        userRepository.save(manager2);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act - Get users filtered by role (MANAGER)
        ResponseEntity<UserListResponse> filterResponse = restTemplate.exchange(
                "/users?role=MANAGER&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                UserListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, filterResponse.getStatusCode());
        assertEquals(2, filterResponse.getBody().getUsers().size());
        assertTrue(filterResponse.getBody().getUsers().stream()
                .allMatch(u -> u.getRole().equals(UserRole.MANAGER)));
    }


    // ========================================================================
    // WORKFLOW TEST 4: BULK DELETE
    // ========================================================================
    @Test
    @DisplayName("BULK DELETE FLOW - Admin Bulk Deletes Multiple Users")
    void testBulkDeleteWorkflow() {
        // Arrange - Create users to delete
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .username("bulk_user_1")
                .email("bulk1@example.com")
                .password("hashed_password")
                .firstName("Bulk")
                .lastName("One")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("bulk_user_2")
                .email("bulk2@example.com")
                .password("hashed_password")
                .firstName("Bulk")
                .lastName("Two")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        BulkDeleteRequest bulkDeleteRequest = BulkDeleteRequest.builder()
                .userIds(List.of(user1.getId(), user2.getId()))
                .build();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act
        ResponseEntity<BulkDeleteResponse> deleteResponse = restTemplate.postForEntity(
                "/users/bulk-delete",
                new HttpEntity<>(bulkDeleteRequest, adminHeaders),
                BulkDeleteResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
        assertEquals(2, deleteResponse.getBody().getSuccessCount());
        assertEquals(0, deleteResponse.getBody().getFailureCount());

        // Verify users are deleted
        assertFalse(userRepository.findById(user1.getId()).isPresent());
        assertFalse(userRepository.findById(user2.getId()).isPresent());
    }


    // ========================================================================
    // AUTHORIZATION TEST 1: REGULAR USER CANNOT CREATE
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Regular User Cannot Create Users")
    void testRegularUserCannotCreateUsers() {
        // Arrange
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username("unauthorized_user")
                .email("unauthorized@example.com")
                .password("Password123!")
                .firstName("Unauthorized")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);

        // Act
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/users",
                new HttpEntity<>(createRequest, userHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, createResponse.getStatusCode());
    }


    // ========================================================================
    // AUTHORIZATION TEST 2: REGULAR USER CAN UPDATE OWN PROFILE
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Regular User Can Update Own Profile")
    void testRegularUserCanUpdateOwnProfile() {
        // Arrange
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("UpdatedJohn")
                .lastName("UpdatedDoe")
                .build();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);

        // Act
        ResponseEntity<UserResponse> updateResponse = restTemplate.exchange(
                "/users/" + regularUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, userHeaders),
                UserResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("UpdatedJohn", updateResponse.getBody().getFirstName());
    }


    // ========================================================================
    // AUTHORIZATION TEST 3: REGULAR USER CANNOT UPDATE OTHERS
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Regular User Cannot Update Other Users")
    void testRegularUserCannotUpdateOthers() {
        // Arrange
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .username("another_user")
                .email("another@example.com")
                .password("hashed_password")
                .firstName("Another")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        anotherUser = userRepository.save(anotherUser);

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("HackedName")
                .build();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);

        // Act
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/users/" + anotherUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, userHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, updateResponse.getStatusCode());
    }


    // ========================================================================
    // AUTHORIZATION TEST 4: REGULAR USER CANNOT DELETE
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Regular User Cannot Delete Users")
    void testRegularUserCannotDeleteUsers() {
        // Arrange
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);

        // Act
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/users/" + regularUser.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(userHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());
    }


    // ========================================================================
    // ERROR SCENARIO TEST 1: CREATE WITH DUPLICATE EMAIL
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Create User with Duplicate Email")
    void testCreateUserWithDuplicateEmail() {
        // Arrange
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username("duplicate_email_user")
                .email("john@example.com") // Already exists
                .password("Password123!")
                .firstName("Duplicate")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/users",
                new HttpEntity<>(createRequest, adminHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.CONFLICT, createResponse.getStatusCode());
        assertTrue(createResponse.getBody().contains("DUPLICATE_RESOURCE"));
    }


    // ========================================================================
    // ERROR SCENARIO TEST 2: UPDATE NON-EXISTENT USER
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Update Non-existent User")
    void testUpdateNonExistentUser() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .build();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/users/" + nonExistentId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, adminHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, updateResponse.getStatusCode());
        assertTrue(updateResponse.getBody().contains("RESOURCE_NOT_FOUND"));
    }


    // ========================================================================
    // ERROR SCENARIO TEST 3: DELETE NON-EXISTENT USER
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Delete Non-existent User")
    void testDeleteNonExistentUser() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/users/" + nonExistentId,
                HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders),
                String.class);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, deleteResponse.getStatusCode());
    }


    // ========================================================================
    // PROFILE VIEW TEST
    // ========================================================================
    @Test
    @DisplayName("PROFILE - User Can View Their Own Profile")
    void testUserCanViewOwnProfile() {
        // Arrange
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAccessToken);

        // Act
        ResponseEntity<UserResponse> profileResponse = restTemplate.exchange(
                "/users/" + regularUser.getId() + "/profile",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                UserResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, profileResponse.getStatusCode());
        assertEquals(regularUser.getId(), profileResponse.getBody().getId());
        assertEquals("john_doe", profileResponse.getBody().getUsername());
    }


    // ========================================================================
    // PAGINATION WITH STATUS FILTER TEST
    // ========================================================================
    @Test
    @DisplayName("FILTER - Get Users by Status")
    void testGetUsersByStatus() {
        // Arrange - Create an inactive user
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .username("inactive_user")
                .email("inactive@example.com")
                .password("hashed_password")
                .firstName("Inactive")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .build();

        userRepository.save(inactiveUser);

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // Act
        ResponseEntity<UserListResponse> statusFilterResponse = restTemplate.exchange(
                "/users?status=INACTIVE",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                UserListResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, statusFilterResponse.getStatusCode());
        assertEquals(1, statusFilterResponse.getBody().getUsers().size());
        assertEquals(UserStatus.INACTIVE, statusFilterResponse.getBody().getUsers().get(0).getStatus());
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:
    Run individual test:
        mvn test -Dtest=UserIntegrationTest#testCompleteCRUDWorkflow
    Run all UserIntegration tests:
        mvn test -Dtest=UserIntegrationTest
    Run with detailed output:
        mvn test -Dtest=UserIntegrationTest -X
    Run specific test pattern:
        mvn test -Dtest=UserIntegrationTest#testAuthorization*
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
    [INFO] Total time: 28.456s

    Note: First run takes longer due to Docker setup
    Subsequent runs reuse container image (~10-15 seconds each)     */
}

