// src/test/java/com/adminportal/controller/AuthControllerTest.java

// ============================================================================
// Testing Strategy:
// - Use @WebMvcTest to load only controller & security layer (no full context)
// - Mock service layer dependencies
// - Test HTTP requests/responses via MockMvc
// - Verify status codes, response bodies, and headers
// - Test authorization with @WithMockUser
// ============================================================================

package com.adminportal.controller;

import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.RefreshTokenRequest;
import com.adminportal.dto.request.RegisterRequest;
import com.adminportal.dto.response.LoginResponse;
import com.adminportal.dto.response.TokenResponse;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.exception.AuthenticationException;
import com.adminportal.exception.DuplicateResourceException;
import com.adminportal.exception.TokenException;
import com.adminportal.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController Unit Tests")
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private LoginResponse loginResponse;
    private TokenResponse tokenResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        // Create login request
        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        // Create register request
        registerRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create refresh token request
        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("refresh_token_abc123")
                .build();

        // Create user response
        userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .joinedDate(LocalDateTime.now())
                .build();

        // Create login response
        loginResponse = LoginResponse.builder()
                .accessToken("access_token_xyz789")
                .refreshToken("refresh_token_abc123")
                .expiresIn(3600)
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        // Create token response
        tokenResponse = TokenResponse.builder()
                .accessToken("new_access_token_123")
                .expiresIn(3600)
                .tokenType("Bearer")
                .build();
    }


    // ========================================================================
    // LOGIN ENDPOINT TESTS - POST /auth/login
    // ========================================================================
    @Test
    @DisplayName("LOGIN - Should return 200 OK with tokens and user data")
    void testLogin_Success() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token_xyz789"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token_abc123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.username").value("john_doe"));

        // Verify service was called
        verify(authService).login(any(LoginRequest.class));
    }


    @Test
    @DisplayName("LOGIN - Should return 401 when credentials are invalid")
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("INVALID_CREDENTIALS", "Email or password is incorrect"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService).login(any(LoginRequest.class));
    }


    @Test
    @DisplayName("LOGIN - Should return 401 when user not found")
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequest badRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("USER_NOT_FOUND", "User not found with email"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        verify(authService).login(any(LoginRequest.class));
    }


    @Test
    @DisplayName("LOGIN - Should return 400 when request is invalid (missing email)")
    void testLogin_InvalidRequest_MissingEmail() throws Exception {
        // Arrange - Create invalid request (missing email)
        String invalidJson = "{ \"password\": \"password123\" }";

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Verify service was NOT called
        verify(authService, org.mockito.Mockito.never()).login(any(LoginRequest.class));
    }


    @Test
    @DisplayName("LOGIN - Should return 400 when password is empty")
    void testLogin_InvalidRequest_EmptyPassword() throws Exception {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }


    // ========================================================================
    // REGISTER ENDPOINT TESTS - POST /auth/register
    // ========================================================================
    @Test
    @DisplayName("REGISTER - Should return 201 Created with tokens and new user data")
    void testRegister_Success() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access_token_xyz789"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token_abc123"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.username").value("john_doe"));

        verify(authService).register(any(RegisterRequest.class));
    }


    @Test
    @DisplayName("REGISTER - Should return 409 Conflict when email already exists")
    void testRegister_DuplicateEmail() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "john@example.com"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").exists());

        verify(authService).register(any(RegisterRequest.class));
    }


    @Test
    @DisplayName("REGISTER - Should return 409 Conflict when username already exists")
    void testRegister_DuplicateUsername() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "username", "john_doe"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        verify(authService).register(any(RegisterRequest.class));
    }


    @Test
    @DisplayName("REGISTER - Should return 400 when request is invalid (weak password)")
    void testRegister_InvalidRequest_WeakPassword() throws Exception {
        // Arrange
        RegisterRequest weakPasswordRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("123") // Too weak
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }


    @Test
    @DisplayName("REGISTER - Should return 400 when email is invalid format")
    void testRegister_InvalidRequest_InvalidEmail() throws Exception {
        // Arrange
        RegisterRequest invalidEmailRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("not_an_email") // Invalid format
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field").isArray());
    }


    // ========================================================================
    // REFRESH TOKEN ENDPOINT TESTS - POST /auth/refresh
    // ========================================================================
    @Test
    @DisplayName("REFRESH - Should return 200 OK with new access token")
    void testRefreshToken_Success() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access_token_123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }


    @Test
    @DisplayName("REFRESH - Should return 401 when refresh token is invalid")
    void testRefreshToken_InvalidToken() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new TokenException("INVALID_TOKEN", "refresh", "Refresh token is invalid"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }


    @Test
    @DisplayName("REFRESH - Should return 401 when refresh token is expired")
    void testRefreshToken_TokenExpired() throws Exception {
        // Arrange
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new TokenException("TOKEN_EXPIRED", "refresh", "Refresh token has expired"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }


    @Test
    @DisplayName("REFRESH - Should return 400 when refresh token is missing")
    void testRefreshToken_MissingToken() throws Exception {
        // Arrange
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }


    // ========================================================================
    // LOGOUT ENDPOINT TESTS - POST /auth/logout
    // ========================================================================
    @Test
    @DisplayName("LOGOUT - Should return 200 OK when logout succeeds")
    void testLogout_Success() throws Exception {
        // Arrange - No return value needed for logout
        when(authService.logout(anyString()))
                .thenReturn(null); // Logout typically returns void

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authService).logout(anyString());
    }


    @Test
    @DisplayName("LOGOUT - Should return 401 when refresh token is invalid")
    void testLogout_InvalidToken() throws Exception {
        // Arrange
        when(authService.logout(anyString()))
                .thenThrow(new TokenException("INVALID_TOKEN", "refresh", "Token not found"));

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verify(authService).logout(anyString());
    }


    @Test
    @DisplayName("LOGOUT - Should return 400 when token is missing")
    void testLogout_MissingToken() throws Exception {
        // Arrange
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                .refreshToken("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    // ========================================================================
    // RESPONSE CONTENT TYPE & HEADER TESTS
    // ========================================================================
    @Test
    @DisplayName("LOGIN - Should return JSON content type")
    void testLogin_ResponseContentType() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }


    @Test
    @DisplayName("REGISTER - Should include proper headers in response")
    void testRegister_ResponseHeaders() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Content-Type"));
    }


    // ========================================================================
    // ERROR RESPONSE STRUCTURE TESTS
    // ========================================================================
    @Test
    @DisplayName("LOGIN - Error response should include code, message, and timestamp")
    void testLogin_ErrorResponseStructure() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("AUTH_FAILED", "Authentication failed"));

        // Act
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        // Assert response contains all required fields
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("code"));
        assertTrue(responseBody.contains("message"));
        assertTrue(responseBody.contains("timestamp"));
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:    
    Run individual test:
        mvn test -Dtest=AuthControllerTest#testLogin_Success
    Run all AuthController tests:
        mvn test -Dtest=AuthControllerTest
    Run with detailed output:
        mvn test -Dtest=AuthControllerTest -X
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0  */
    
}


