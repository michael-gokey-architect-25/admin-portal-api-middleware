// src/test/java/com/adminportal/integration/AuthIntegrationTest.java

// ============================================================================
// Testing Strategy:
// - Use @SpringBootTest for full application context
// - Real database (Testcontainers PostgreSQL)
// - Test complete authentication workflows
// - Real HTTP requests via TestRestTemplate
// - Verify token generation, validation, and refresh flows
// - Test end-to-end login → protected endpoint → refresh → logout
// ============================================================================

package com.adminportal.integration;

import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.RefreshTokenRequest;
import com.adminportal.dto.request.RegisterRequest;
import com.adminportal.dto.response.LoginResponse;
import com.adminportal.dto.response.TokenResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auth Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

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
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        userRepository.deleteAll();

        // Create register request
        registerRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("SecurePassword123!")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create login request (after registration)
        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("SecurePassword123!")
                .build();

        // Create test user for pre-populated tests
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("test_user")
                .email("test@example.com")
                .password("$2a$10$slYQmyNdGzin7olVN7q2OPST9/PgBkqquzi8Ay0IQi1VG5e.CCcga") // hashed: TestPassword123!
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    // ========================================================================
    // WORKFLOW TEST 1: COMPLETE REGISTRATION → LOGIN → PROTECTED → LOGOUT
    // ========================================================================
    @Test
    @DisplayName("COMPLETE FLOW - Register → Login → Access Protected → Logout")
    void testCompleteAuthenticationFlow() {
        // STEP 1: Register new user
        ResponseEntity<LoginResponse> registerResponse = restTemplate.postForEntity(
                "/auth/register",
                registerRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        LoginResponse registerResponseBody = registerResponse.getBody();
        
        assertNotNull(registerResponseBody.getAccessToken());
        assertNotNull(registerResponseBody.getRefreshToken());
        assertEquals("john@example.com", registerResponseBody.getUser().getEmail());
        String accessTokenFromRegister = registerResponseBody.getAccessToken();

        // STEP 2: Use access token to access protected endpoint (GET /users/{id})
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessTokenFromRegister);

        ResponseEntity<Object> protectedResponse = restTemplate.getForEntity(
                "/users/" + registerResponseBody.getUser().getId(),
                Object.class,
                headers);

        assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());
        assertNotNull(protectedResponse.getBody());

        // STEP 3: Logout with refresh token
        RefreshTokenRequest logoutRequest = RefreshTokenRequest.builder()
                .refreshToken(registerResponseBody.getRefreshToken())
                .build();

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/auth/logout",
                logoutRequest,
                Void.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());

        // STEP 4: Verify refresh token is invalidated (should fail)
        ResponseEntity<String> invalidTokenResponse = restTemplate.postForEntity(
                "/auth/refresh",
                logoutRequest,
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, invalidTokenResponse.getStatusCode());
    }


    // ========================================================================
    // WORKFLOW TEST 2: LOGIN → REFRESH TOKEN → NEW ACCESS TOKEN
    // ========================================================================
    @Test
    @DisplayName("TOKEN REFRESH FLOW - Login → Get Tokens → Refresh Access Token")
    void testTokenRefreshFlow() {
        // STEP 1: Login with pre-created user
        userRepository.save(testUser);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        LoginResponse loginResponseBody = loginResponse.getBody();

        String originalAccessToken = loginResponseBody.getAccessToken();
        String refreshToken = loginResponseBody.getRefreshToken();

        // STEP 2: Wait a moment to ensure token timestamps differ (optional)
        // Thread.sleep(1000);

        // STEP 3: Refresh the access token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        ResponseEntity<TokenResponse> refreshResponse = restTemplate.postForEntity(
                "/auth/refresh",
                refreshRequest,
                TokenResponse.class);

        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody());
        TokenResponse refreshResponseBody = refreshResponse.getBody();

        String newAccessToken = refreshResponseBody.getAccessToken();

        // Verify new token is different from original
        assertNotEquals(originalAccessToken, newAccessToken);
        assertNotNull(newAccessToken);

        // STEP 4: Verify new access token is valid by accessing protected endpoint
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(newAccessToken);

        ResponseEntity<Object> protectedResponse = restTemplate.getForEntity(
                "/users/" + testUser.getId(),
                Object.class,
                headers);

        assertEquals(HttpStatus.OK, protectedResponse.getStatusCode());
    }


    // ========================================================================
    // ERROR SCENARIO TEST 1: INVALID CREDENTIALS
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Login with Invalid Credentials")
    void testLoginWithInvalidCredentials() {
        // Arrange
        userRepository.save(testUser);

        LoginRequest badRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword123!")
                .build();

        // Act
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                badRequest,
                String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("INVALID_CREDENTIALS"));
    }


    // ========================================================================
    // ERROR SCENARIO TEST 2: USER NOT FOUND
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Login with Non-existent User")
    void testLoginWithNonExistentUser() {
        // Arrange
        LoginRequest badRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("AnyPassword123!")
                .build();

        // Act
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                badRequest,
                String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());
    }


    // ========================================================================
    // ERROR SCENARIO TEST 3: DUPLICATE REGISTRATION
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Register with Duplicate Email")
    void testRegisterWithDuplicateEmail() {
        // STEP 1: Register first user successfully
        ResponseEntity<LoginResponse> firstResponse = restTemplate.postForEntity(
                "/auth/register",
                registerRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());

        // STEP 2: Try to register again with same email
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username("different_username")
                .email("john@example.com") // Same email
                .password("DifferentPassword123!")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                "/auth/register",
                duplicateRequest,
                String.class);

        // Assert
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        assertTrue(duplicateResponse.getBody().contains("DUPLICATE_RESOURCE"));
    }


    // ========================================================================
    // ERROR SCENARIO TEST 4: EXPIRED REFRESH TOKEN
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Refresh with Expired Token")
    void testRefreshWithExpiredToken() {
        // Arrange
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                .refreshToken("invalid_or_expired_token_xyz")
                .build();

        // Act
        ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                "/auth/refresh",
                invalidRequest,
                String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());
        assertTrue(refreshResponse.getBody().contains("TOKEN_EXPIRED") || 
                   refreshResponse.getBody().contains("INVALID_TOKEN"));
    }


    // ========================================================================
    // ERROR SCENARIO TEST 5: INVALID REFRESH TOKEN
    // ========================================================================
    @Test
    @DisplayName("ERROR FLOW - Refresh with Invalid Token Format")
    void testRefreshWithInvalidTokenFormat() {
        // Arrange
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                .refreshToken("not.a.valid.jwt")
                .build();

        // Act
        ResponseEntity<String> refreshResponse = restTemplate.postForEntity(
                "/auth/refresh",
                invalidRequest,
                String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());
    }


    // ========================================================================
    // AUTHORIZATION TEST 1: ACCESS PROTECTED ENDPOINT WITHOUT TOKEN
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Access Protected Endpoint Without Token")
    void testAccessProtectedEndpointWithoutToken() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        // Act - Try to access protected endpoint without token
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/users/" + savedUser.getId(),
                String.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ========================================================================
    // AUTHORIZATION TEST 2: ACCESS PROTECTED ENDPOINT WITH INVALID TOKEN
    // ========================================================================
    @Test
    @DisplayName("AUTHORIZATION - Access Protected Endpoint with Invalid Token")
    void testAccessProtectedEndpointWithInvalidToken() {
        // Arrange
        User savedUser = userRepository.save(testUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.jwt.token");

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/users/" + savedUser.getId(),
                String.class,
                headers);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // ========================================================================
    // MULTI-DEVICE LOGIN TEST
    // ========================================================================
    @Test
    @DisplayName("MULTI-DEVICE - Multiple Login Sessions from Different Devices")
    void testMultiDeviceLoginSessions() {
        // STEP 1: Login from Device 1
        userRepository.save(testUser);

        ResponseEntity<LoginResponse> device1Login = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.OK, device1Login.getStatusCode());
        String device1AccessToken = device1Login.getBody().getAccessToken();
        String device1RefreshToken = device1Login.getBody().getRefreshToken();

        // STEP 2: Login from Device 2 (same user, different session)
        ResponseEntity<LoginResponse> device2Login = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.OK, device2Login.getStatusCode());
        String device2AccessToken = device2Login.getBody().getAccessToken();
        String device2RefreshToken = device2Login.getBody().getRefreshToken();

        // STEP 3: Verify both sessions are independent
        // Different tokens
        assertNotEquals(device1AccessToken, device2AccessToken);
        assertNotEquals(device1RefreshToken, device2RefreshToken);

        // STEP 4: Verify both tokens work
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(device1AccessToken);

        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(device2AccessToken);

        ResponseEntity<Object> response1 = restTemplate.getForEntity(
                "/users/" + testUser.getId(),
                Object.class,
                headers1);

        ResponseEntity<Object> response2 = restTemplate.getForEntity(
                "/users/" + testUser.getId(),
                Object.class,
                headers2);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }


    // ========================================================================
    // LOGOUT AND SESSION INVALIDATION TEST
    // ========================================================================
    @Test
    @DisplayName("SESSION - Logout Invalidates Session")
    void testLogoutInvalidatesSession() {
        // STEP 1: Register and get tokens
        ResponseEntity<LoginResponse> registerResponse = restTemplate.postForEntity(
                "/auth/register",
                registerRequest,
                LoginResponse.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        LoginResponse loginData = registerResponse.getBody();
        String accessToken = loginData.getAccessToken();
        String refreshToken = loginData.getRefreshToken();

        // STEP 2: Verify access token works
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Object> beforeLogout = restTemplate.getForEntity(
                "/users/" + loginData.getUser().getId(),
                Object.class,
                headers);

        assertEquals(HttpStatus.OK, beforeLogout.getStatusCode());

        // STEP 3: Logout
        RefreshTokenRequest logoutRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/auth/logout",
                logoutRequest,
                Void.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());

        // STEP 4: Verify refresh token is invalidated
        ResponseEntity<String> refreshAfterLogout = restTemplate.postForEntity(
                "/auth/refresh",
                logoutRequest,
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, refreshAfterLogout.getStatusCode());
    }


    // ========================================================================
    // RESPONSE VALIDATION TESTS
    // ========================================================================
    @Test
    @DisplayName("RESPONSE - Login Response Contains All Required Fields")
    void testLoginResponseStructure() {
        // Arrange
        userRepository.save(testUser);

        // Act
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                LoginResponse.class);

        // Assert
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        LoginResponse body = loginResponse.getBody();

        assertNotNull(body.getAccessToken());
        assertNotNull(body.getRefreshToken());
        assertNotNull(body.getTokenType());
        assertNotNull(body.getUser());
        assertNotNull(body.getUser().getId());
        assertNotNull(body.getUser().getEmail());
        assertEquals("john@example.com", body.getUser().getEmail());
    }


    @Test
    @DisplayName("RESPONSE - Register Response Contains New User Data")
    void testRegisterResponseStructure() {
        // Act
        ResponseEntity<LoginResponse> registerResponse = restTemplate.postForEntity(
                "/auth/register",
                registerRequest,
                LoginResponse.class);

        // Assert
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        LoginResponse body = registerResponse.getBody();

        assertNotNull(body.getUser().getId());
        assertEquals("john_doe", body.getUser().getUsername());
        assertEquals("john@example.com", body.getUser().getEmail());
        assertEquals(UserRole.USER, body.getUser().getRole());
        assertEquals(UserStatus.ACTIVE, body.getUser().getStatus());
    }


    // ========================================================================
    // HEALTH CHECK TEST
    // ========================================================================
    @Test
    @DisplayName("HEALTH - Health endpoint is publicly accessible")
    void testHealthCheckPublicAccess() {
        // Act
        ResponseEntity<Object> healthResponse = restTemplate.getForEntity(
                "/health",
                Object.class);

        // Assert
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertNotNull(healthResponse.getBody());
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:
    // ========================================================================
    Run individual test:
        mvn test -Dtest=AuthIntegrationTest#testCompleteAuthenticationFlow
    Run all AuthIntegration tests:
        mvn test -Dtest=AuthIntegrationTest
    Run with detailed output:
        mvn test -Dtest=AuthIntegrationTest -X
    Run specific test pattern:
        mvn test -Dtest=AuthIntegrationTest#testTokenRefresh*
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
    [INFO] Total time: 25.345s

    Note: First run takes longer due to Docker setup
    Subsequent runs reuse container image (~10-15 seconds each)     */
}


