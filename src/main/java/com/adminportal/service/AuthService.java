// admin-portal-api/src/main/java/com/adminportal/service/AuthService.java

// ============================================================================
// PURPOSE: Authentication service - handles login, registration, token management
// - Validates user credentials
// - Generates JWT tokens
// - Manages refresh token lifecycle
// - Handles logout (token revocation)
// KEY CONCEPTS:
// 1. Service Layer: Contains business logic (not in controller)
// 2. Single Responsibility: AuthService handles only auth logic
// 3. Dependency Injection: Dependencies passed via constructor
// 4. Exception Handling: Throws custom exceptions (caught by GlobalExceptionHandler)
// 5. Transaction Management: @Transactional ensures data consistency
// TEACHING NOTES:
// - Services are stateless (can be used by multiple threads)
// - Always use @Transactional for operations modifying database
// - Validate input before database operations
// - Use custom exceptions for business errors (not RuntimeException)
// - Never log sensitive data (passwords, tokens)
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
import com.adminportal.exception.ResourceNotFoundException;
import com.adminportal.mapper.UserMapper;
import com.adminportal.repository.RefreshTokenRepository;
import com.adminportal.repository.UserRepository;
import com.adminportal.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication Service
 * RESPONSIBILITIES:
 * - User login (validate credentials, generate tokens)
 * - User registration (create account, hash password)
 * - Token refresh (generate new access token)
 * - User logout (revoke refresh token)
 * - Check authentication status
 * WORKFLOW:
 * 1. Client sends login request with email + password
 * 2. AuthService validates against database
 * 3. If valid, generates JWT access + refresh tokens
 * 4. Client stores tokens and includes in future requests
 * 5. JWT filter validates token on each request
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    /** ==================== DEPENDENCIES ====================
    // Injected by Spring constructor */
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    
 
    /** ==================== LOGIN ====================
     * User login - authenticate with email/password
     *
     * FLOW:
     * 1. Validate email is not blank
     * 2. Load user from database
     * 3. Check if account is active (not suspended/inactive)
     * 4. Authenticate using Spring AuthenticationManager
     * 5. Generate JWT tokens
     * 6. Store refresh token in database
     * 7. Update last_login_date
     * 8. Return tokens + user data
     *
     * @param loginRequest Email and password from client
     * @return LoginResponse with accessToken, refreshToken, user data
     * @throws ResourceNotFoundException if user not found
     * @throws AuthenticationException if account not active or password wrong */
     
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.email());

        // ==================== STEP 1: VALIDATE INPUT ====================
        if (loginRequest.email() == null || loginRequest.email().isBlank()) {
            log.warn("Login attempt with blank email");
            throw new AuthenticationException("Email and password are required");
        }

        // ==================== STEP 2: FIND USER ====================
        // Try email first (primary login identifier)
        User user = userRepository.findByEmail(loginRequest.email())
            .orElseThrow(() -> {
                log.warn("Login failed: User not found with email: {}", loginRequest.email());
                return new ResourceNotFoundException("User not found");
            });

        // ==================== STEP 3: CHECK ACCOUNT STATUS ====================
        // Users cannot login if suspended or inactive
        if (!user.isActive()) {
            log.warn("Login failed: Account not active for user: {} (status: {})", 
                user.getEmail(), user.getStatus());
            throw new AuthenticationException(
                "Account is " + user.getStatus().toString().toLowerCase() + 
                ". Please contact administrator."
            );
        }

        // ==================== STEP 4: AUTHENTICATE ====================
        // Spring AuthenticationManager validates password using PasswordEncoder
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.email(),
                    loginRequest.password()
                )
            );
            log.debug("User authenticated successfully: {}", loginRequest.email());

            // ==================== STEP 5: GENERATE TOKENS ====================
            // Create JWT tokens (access = short-lived, refresh = long-lived)
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

            // ==================== STEP 6: STORE REFRESH TOKEN ====================
            // Refresh token must be stored in DB for validation on refresh
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))  // 7 day expiration
                .build();
            refreshTokenRepository.save(refreshTokenEntity);
            log.debug("Refresh token stored in database for user: {}", user.getEmail());

            // ==================== STEP 7: UPDATE LAST LOGIN ====================
            // Track user activity
            user.setLastLoginDate(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last_login_date for user: {}", user.getEmail());

            // ==================== STEP 8: BUILD RESPONSE ====================
            // Return tokens + user info (password excluded)
            LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600000L)  // 60 minutes in milliseconds
                .user(userMapper.toUserResponse(user))
                .build();

            log.info("Login successful for user: {}", user.getEmail());
            return response;

        } catch (org.springframework.security.core.AuthenticationException ex) {
            // AuthenticationManager throws this if password wrong
            log.warn("Authentication failed for user: {} - Invalid password", loginRequest.email());
            throw new AuthenticationException("Invalid email or password");
        }
    }



    /** ==================== REGISTRATION ====================
     * User registration - create new account
     * 1. Validate input (email format, password strength)
     * 2. Check if email already exists
     * 3. Check if username already exists
     * 4. Hash password using bcrypt
     * 5. Create user entity with default role (USER)
     * 6. Save to database
     * 7. Generate JWT tokens
     * 8. Return tokens + user data
     * @param registerRequest First name, last name, email, password
     * @return LoginResponse with tokens for newly created user
     * @throws DuplicateResourceException if email or username exists
     * @throws AuthenticationException if validation fails   */
    @Transactional
    public LoginResponse register(RegisterRequest registerRequest) {
        log.info("Registration attempt for email: {}", registerRequest.email());

        // ==================== STEP 1: VALIDATE INPUT ====================
        if (registerRequest.email() == null || registerRequest.email().isBlank()) {
            throw new AuthenticationException("Email is required");
        }
        if (registerRequest.password() == null || registerRequest.password().length() < 8) {
            throw new AuthenticationException("Password must be at least 8 characters");
        }

        // ==================== STEP 2: CHECK EMAIL UNIQUENESS ====================
        if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
            log.warn("Registration failed: Email already exists: {}", registerRequest.email());
            throw new DuplicateResourceException("Email already registered");
        }

        // ==================== STEP 3: CHECK USERNAME UNIQUENESS ====================
        // Generate username from email (e.g., alice@example.com → alice)
        String generatedUsername = registerRequest.email().split("@")[0];
        if (userRepository.findByUsername(generatedUsername).isPresent()) {
            // If generated username exists, add timestamp suffix
            generatedUsername = generatedUsername + "_" + System.currentTimeMillis();
        }

        // ==================== STEP 4: HASH PASSWORD ====================
        // BCryptPasswordEncoder with strength 12 (expensive, slow, secure)
        String hashedPassword = passwordEncoder.encode(registerRequest.password());
        log.debug("Password hashed for new user: {}", registerRequest.email());

        // ==================== STEP 5: CREATE USER ENTITY ====================
        // New users get USER role by default, ACTIVE status
        User newUser = User.builder()
            .email(registerRequest.email())
            .username(generatedUsername)
            .password(hashedPassword)
            .firstName(registerRequest.firstName())
            .lastName(registerRequest.lastName())
            .role(UserRole.USER)              // Default role
            .status(UserStatus.ACTIVE)        // Default status
            .canManageUsers(false)            // Default permissions: none
            .canViewReports(false)
            .canManageSettings(false)
            .build();

        // ==================== STEP 6: SAVE TO DATABASE ====================
        User savedUser = userRepository.save(newUser);
        log.info("New user registered: {}", savedUser.getEmail());

        // ==================== STEP 7: AUTO-LOGIN ====================
        // Create authentication from saved user
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            savedUser.getUsername(),
            null,
            savedUser.getRole() != null ? 
                java.util.List.of(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + savedUser.getRole().name()
                    )
                ) : java.util.List.of()
        );

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getUsername());

        // ==================== STEP 8: STORE REFRESH TOKEN ====================
        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .user(savedUser)
            .token(refreshToken)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // ==================== STEP 9: BUILD RESPONSE ====================
        LoginResponse response = LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(3600000L)  // 60 minutes
            .user(userMapper.toUserResponse(savedUser))
            .build();

        log.info("Registration successful for user: {}", savedUser.getEmail());
        return response;
    }

 
    /** ==================== REFRESH TOKEN ====================
     * Refresh access token using refresh token
     * 1. Validate refresh token format
     * 2. Find refresh token in database
     * 3. Check if token is not expired and not revoked
     * 4. Load associated user
     * 5. Generate new access token
     * 6. Return new access token
     * @param refreshTokenRequest Refresh token from client
     * @return TokenResponse with new access token
     * @throws AuthenticationException if token invalid/expired/revoked
     * @throws ResourceNotFoundException if token not found  */
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.debug("Token refresh attempt");

        // ==================== STEP 1: VALIDATE INPUT ====================
        if (refreshTokenRequest.refreshToken() == null || 
            refreshTokenRequest.refreshToken().isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        // ==================== STEP 2: FIND TOKEN IN DATABASE ====================
        // Refresh token must exist in DB for it to be valid
        RefreshToken refreshToken = refreshTokenRepository
            .findByToken(refreshTokenRequest.refreshToken())
            .orElseThrow(() -> {
                log.warn("Refresh token not found in database");
                return new AuthenticationException("Invalid refresh token");
            });

        // ==================== STEP 3: CHECK TOKEN STATUS ====================
        // Token must not be expired
        if (refreshToken.isExpired()) {
            log.warn("Refresh token expired: {}", refreshToken.getId());
            throw new AuthenticationException("Refresh token has expired");
        }

        // Token must not be revoked (logout)
        if (refreshToken.isRevoked()) {
            log.warn("Refresh token revoked: {}", refreshToken.getId());
            throw new AuthenticationException("Refresh token has been revoked");
        }

        // ==================== STEP 4: LOAD USER ====================
        User user = refreshToken.getUser();
        if (user == null || !user.isActive()) {
            log.warn("User account inactive or deleted");
            throw new AuthenticationException("User account is not active");
        }

        // ==================== STEP 5: GENERATE NEW ACCESS TOKEN ====================
        String newAccessToken = jwtTokenProvider.generateAccessTokenFromUserId(
            user.getId(),
            user.getUsername(),
            user.getRole().name()
        );
        log.debug("New access token generated for user: {}", user.getEmail());

        // ==================== STEP 6: BUILD RESPONSE ====================
        TokenResponse response = TokenResponse.builder()
            .accessToken(newAccessToken)
            .expiresIn(3600000L)  // 60 minutes
            .build();

        log.info("Token refresh successful for user: {}", user.getEmail());
        return response;
    }

    

    /** ==================== LOGOUT ====================
     * User logout - revoke refresh token
     * 1. Extract refresh token from request (passed by client)
     * 2. Find token in database
     * 3. Mark token as revoked (set revoked_at timestamp)
     * 4. Save to database
     * 5. Return success
     * @param userId User ID of user logging out
     * @param refreshToken Refresh token to revoke
     * @throws ResourceNotFoundException if token not found */
     
    @Transactional
    public void logout(UUID userId, String refreshToken) {
        log.info("Logout attempt for user: {}", userId);

        // ==================== STEP 1: VALIDATE INPUT ====================
        if (refreshToken == null || refreshToken.isBlank()) {
            log.debug("Logout called without refresh token");
            return;  // Silently succeed (client may not have token)
        }

        // ==================== STEP 2: FIND TOKEN ====================
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElse(null);

        if (token == null) {
            log.debug("Refresh token not found, already revoked or expired");
            return;  // Silently succeed
        }

        // ==================== STEP 3: VERIFY OWNERSHIP ====================
        // Security: ensure user can only revoke their own tokens
        if (!token.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to revoke token of user {}", 
                userId, token.getUser().getId());
            throw new AuthenticationException("Cannot revoke other user's tokens");
        }

        // ==================== STEP 4: REVOKE TOKEN ====================
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        log.info("Token revoked successfully for user: {}", userId);
    }

    
    /**  ==================== CHECK AUTH STATUS ====================
     * Check if refresh token is valid (used on app initialization)
     * 1. Validate token format
     * 2. Find token in database
     * 3. Check if not expired and not revoked
     * 4. Return user data if valid
     * @param refreshToken Refresh token to validate
     * @return User data if token valid
     * @throws AuthenticationException if token invalid */
      
    public User validateRefreshToken(String refreshToken) {
        log.debug("Validating refresh token");

        // ==================== STEP 1: VALIDATE INPUT ====================
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        // ==================== STEP 2: FIND TOKEN ====================
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        // ==================== STEP 3: CHECK STATUS ====================
        if (token.isExpired()) {
            throw new AuthenticationException("Refresh token expired");
        }
        if (token.isRevoked()) {
            throw new AuthenticationException("Refresh token revoked");
        }

        // ==================== STEP 4: RETURN USER ====================
        return token.getUser();
    }
}

