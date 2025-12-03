// src/test/java/com/adminportal/service/AuthServiceTest.java

// ============================================================================
// Testing Strategy:
// - Mock all dependencies (repositories, encoder, token provider, mapper)
// - No database calls
// - Test success and failure scenarios
// - Use @ExtendWith(MockitoExtension.class) for Mockito integration
// ============================================================================

package com.adminportal.service;

import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.RefreshTokenRequest;
import com.adminportal.dto.request.RegisterRequest;
import com.adminportal.dto.response.LoginResponse;
import com.adminportal.dto.response.TokenResponse;
import com.adminportal.entity.RefreshToken;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.exception.AuthenticationException;
import com.adminportal.exception.DuplicateResourceException;
import com.adminportal.exception.TokenException;
import com.adminportal.repository.RefreshTokenRepository;
import com.adminportal.repository.UserRepository;
import com.adminportal.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthService Unit Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // ==================================================================
    // MOCKED DEPENDENCIES
    // ==================================================================
    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Create test user
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

        // Create login request
        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("plain_password_123")
                .build();

        // Create register request
        registerRequest = RegisterRequest.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .password("password_123")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        // Create test refresh token
        testRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token("refresh_token_abc123")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }


    // ========================================================================
    // LOGIN TESTS
    // ========================================================================
    @Test
    @DisplayName("LOGIN - Should successfully login with correct credentials")
    void testLogin_Success() {
        // Arrange
        String accessToken = "access_token_xyz789";
        String refreshToken = "refresh_token_abc123";

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches("plain_password_123", "hashed_password_123"))
                .thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(testUser))
                .thenReturn(accessToken);

        when(jwtTokenProvider.generateRefreshToken(testUser))
                .thenReturn(refreshToken);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());

        // Verify methods were called
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(jwtTokenProvider, times(1)).generateAccessToken(testUser);
        verify(jwtTokenProvider, times(1)).generateRefreshToken(testUser);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }


    @Test
    @DisplayName("LOGIN - Should throw exception when user not found")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        LoginRequest badRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("any_password")
                .build();

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> {
            authService.login(badRequest);
        });

        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }


    @Test
    @DisplayName("LOGIN - Should throw exception when password is incorrect")
    void testLogin_IncorrectPassword() {
        // Arrange
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches("wrong_password", "hashed_password_123"))
                .thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("INVALID_CREDENTIALS", exception.getCode());
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }


    @Test
    @DisplayName("LOGIN - Should throw exception when user is inactive")
    void testLogin_InactiveUser() {
        // Arrange
        testUser.setStatus(UserStatus.INACTIVE);

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches("plain_password_123", "hashed_password_123"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> {
            authService.login(loginRequest);
        });

        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }


    // ========================================================================
    // REGISTER TESTS
    // ========================================================================
    @Test
    @DisplayName("REGISTER - Should successfully register new user")
    void testRegister_Success() {
        // Arrange
        String accessToken = "access_token_xyz789";
        String refreshToken = "refresh_token_abc123";

        when(userRepository.existsByEmail("jane@example.com"))
                .thenReturn(false);

        when(userRepository.existsByUsername("jane_doe"))
                .thenReturn(false);

        when(passwordEncoder.encode("password_123"))
                .thenReturn("hashed_password_456");

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .username("jane_doe")
                .email("jane@example.com")
                .password("hashed_password_456")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(newUser);

        when(jwtTokenProvider.generateAccessToken(newUser))
                .thenReturn(accessToken);

        when(jwtTokenProvider.generateRefreshToken(newUser))
                .thenReturn(refreshToken);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // Act
        LoginResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());

        // Verify user was saved with correct fields
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertEquals("jane_doe", savedUser.getUsername());
        assertEquals("jane@example.com", savedUser.getEmail());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
    }


    @Test
    @DisplayName("REGISTER - Should throw exception when email already exists")
    void testRegister_DuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail("jane@example.com"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("REGISTER - Should throw exception when username already exists")
    void testRegister_DuplicateUsername() {
        // Arrange
        when(userRepository.existsByEmail("jane@example.com"))
                .thenReturn(false);

        when(userRepository.existsByUsername("jane_doe"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any(User.class));
    }


    // ========================================================================
    // REFRESH TOKEN TESTS
    // ========================================================================
    @Test
    @DisplayName("REFRESH TOKEN - Should generate new access token")
    void testRefreshToken_Success() {
        // Arrange
        String newAccessToken = "new_access_token_123";
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh_token_abc123")
                .build();

        when(refreshTokenRepository.findByToken("refresh_token_abc123"))
                .thenReturn(Optional.of(testRefreshToken));

        when(jwtTokenProvider.generateAccessToken(testUser))
                .thenReturn(newAccessToken);

        // Act
        TokenResponse response = authService.refreshToken(request);

        // Assert
        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());

        verify(refreshTokenRepository, times(1)).findByToken("refresh_token_abc123");
        verify(jwtTokenProvider, times(1)).generateAccessToken(testUser);
    }


    @Test
    @DisplayName("REFRESH TOKEN - Should throw exception when token invalid")
    void testRefreshToken_InvalidToken() {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid_token")
                .build();

        when(refreshTokenRepository.findByToken("invalid_token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TokenException.class, () -> {
            authService.refreshToken(request);
        });

        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }


    @Test
    @DisplayName("REFRESH TOKEN - Should throw exception when token expired")
    void testRefreshToken_TokenExpired() {
        // Arrange
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh_token_abc123")
                .build();

        when(refreshTokenRepository.findByToken("refresh_token_abc123"))
                .thenReturn(Optional.of(testRefreshToken));

        // Act & Assert
        TokenException exception = assertThrows(TokenException.class, () -> {
            authService.refreshToken(request);
        });

        assertEquals("TOKEN_EXPIRED", exception.getCode());
        assertEquals("refresh", exception.getTokenType());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }


    @Test
    @DisplayName("REFRESH TOKEN - Should throw exception when token revoked")
    void testRefreshToken_TokenRevoked() {
        // Arrange
        testRefreshToken.setRevokedAt(LocalDateTime.now()); // Token is revoked

        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh_token_abc123")
                .build();

        when(refreshTokenRepository.findByToken("refresh_token_abc123"))
                .thenReturn(Optional.of(testRefreshToken));

        // Act & Assert
        assertThrows(TokenException.class, () -> {
            authService.refreshToken(request);
        });

        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }


    // ========================================================================
    // LOGOUT TESTS
    // ========================================================================
    @Test
    @DisplayName("LOGOUT - Should revoke refresh token")
    void testLogout_Success() {
        // Arrange
        String refreshToken = "refresh_token_abc123";

        when(refreshTokenRepository.findByToken(refreshToken))
                .thenReturn(Optional.of(testRefreshToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // Act
        authService.logout(refreshToken);

        // Assert
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken revokedToken = tokenCaptor.getValue();

        assertNotNull(revokedToken.getRevokedAt());

        verify(refreshTokenRepository, times(1)).findByToken(refreshToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }


    @Test
    @DisplayName("LOGOUT - Should throw exception when token not found")
    void testLogout_TokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("invalid_token"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TokenException.class, () -> {
            authService.logout("invalid_token");
        });

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }



    // ========================================================================
    // VALIDATE REFRESH TOKEN TESTS
    // ========================================================================
    @Test
    @DisplayName("VALIDATE REFRESH TOKEN - Should return true for valid token")
    void testValidateRefreshToken_Valid() {
        // Arrange
        when(refreshTokenRepository.findByToken("refresh_token_abc123"))
                .thenReturn(Optional.of(testRefreshToken));

        // Act
        boolean isValid = authService.validateRefreshToken("refresh_token_abc123");

        // Assert
        assertTrue(isValid);
        verify(refreshTokenRepository, times(1)).findByToken("refresh_token_abc123");
    }


    @Test
    @DisplayName("VALIDATE REFRESH TOKEN - Should return false for invalid token")
    void testValidateRefreshToken_Invalid() {
        // Arrange
        when(refreshTokenRepository.findByToken("invalid_token"))
                .thenReturn(Optional.empty());

        // Act
        boolean isValid = authService.validateRefreshToken("invalid_token");

        // Assert
        assertFalse(isValid);
    }


    @Test
    @DisplayName("VALIDATE REFRESH TOKEN - Should return false for expired token")
    void testValidateRefreshToken_Expired() {
        // Arrange
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(refreshTokenRepository.findByToken("refresh_token_abc123"))
                .thenReturn(Optional.of(testRefreshToken));

        // Act
        boolean isValid = authService.validateRefreshToken("refresh_token_abc123");

        // Assert
        assertFalse(isValid);
    }
}



/* =================================================
 TEST EXECUTION TIPS:
1. Run individual test:
   mvn test -Dtest=AuthServiceTest#testLogin_Success
2. Run all AuthService tests:
   mvn test -Dtest=AuthServiceTest
3. Run with coverage:
   mvn test jacoco:report
4. Run in IDE: Right-click on test class → Run
5. Expected output:
   [INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
*/

