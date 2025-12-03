// src/test/java/com/adminportal/fixtures/TestDataBuilders.java

// ============================================================================
// TEST DATA BUILDERS - Fluent API for Creating Test Fixtures
// Usage: User testUser = UserTestBuilder.aUser().withRole(ADMIN).build();
// UserResponse userResponse = UserResponseTestBuilder.aUserResponse()
//     .withEmail("test@example.com").build();
// ============================================================================

package com.adminportal.fixtures;

import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.RegisterRequest;
import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.RefreshToken;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

// ============================================================================
// USER TEST BUILDER
// ============================================================================
public class UserTestBuilder {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginDate;

    private UserTestBuilder() {
        // Default values
        this.id = UUID.randomUUID();
        this.username = "testuser";
        this.email = "test@example.com";
        this.password = "hashed_password_123";
        this.firstName = "Test";
        this.lastName = "User";
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now().minusDays(30);
        this.updatedAt = LocalDateTime.now();
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public static UserTestBuilder anAdmin() {
        return new UserTestBuilder().withRole(UserRole.ADMIN).withUsername("admin");
    }

    public static UserTestBuilder aManager() {
        return new UserTestBuilder().withRole(UserRole.MANAGER).withUsername("manager");
    }

    public UserTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserTestBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserTestBuilder withUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public UserTestBuilder withLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
        return this;
    }

    public User build() {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .lastLoginDate(lastLoginDate)
                .build();
    }
}


// ============================================================================
// REFRESH TOKEN TEST BUILDER
// ============================================================================
public class RefreshTokenTestBuilder {
    private UUID id;
    private String token;
    private User user;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;

    private RefreshTokenTestBuilder() {
        this.id = UUID.randomUUID();
        this.token = "refresh_token_" + UUID.randomUUID();
        this.user = UserTestBuilder.aUser().build();
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.revokedAt = null;
        this.createdAt = LocalDateTime.now();
    }

    public static RefreshTokenTestBuilder aRefreshToken() {
        return new RefreshTokenTestBuilder();
    }

    public static RefreshTokenTestBuilder aValidRefreshToken() {
        return new RefreshTokenTestBuilder()
                .withExpiresAt(LocalDateTime.now().plusDays(7))
                .withRevokedAt(null);
    }

    public static RefreshTokenTestBuilder anExpiredRefreshToken() {
        return new RefreshTokenTestBuilder()
                .withExpiresAt(LocalDateTime.now().minusHours(1));
    }

    public static RefreshTokenTestBuilder aRevokedRefreshToken() {
        return new RefreshTokenTestBuilder()
                .withRevokedAt(LocalDateTime.now());
    }

    public RefreshTokenTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public RefreshTokenTestBuilder withToken(String token) {
        this.token = token;
        return this;
    }

    public RefreshTokenTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public RefreshTokenTestBuilder withExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public RefreshTokenTestBuilder withRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
        return this;
    }

    public RefreshTokenTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public RefreshToken build() {
        return RefreshToken.builder()
                .id(id)
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .createdAt(createdAt)
                .build();
    }
}


// ============================================================================
// LOGIN REQUEST TEST BUILDER
// ============================================================================
public class LoginRequestTestBuilder {
    private String email;
    private String password;

    private LoginRequestTestBuilder() {
        this.email = "test@example.com";
        this.password = "test_password_123";
    }

    public static LoginRequestTestBuilder aLoginRequest() {
        return new LoginRequestTestBuilder();
    }

    public LoginRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public LoginRequestTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public LoginRequest build() {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }
}


// ============================================================================
// REGISTER REQUEST TEST BUILDER
// ============================================================================
public class RegisterRequestTestBuilder {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;

    private RegisterRequestTestBuilder() {
        this.username = "newuser";
        this.email = "newuser@example.com";
        this.password = "secure_password_123";
        this.firstName = "New";
        this.lastName = "User";
    }

    public static RegisterRequestTestBuilder aRegisterRequest() {
        return new RegisterRequestTestBuilder();
    }

    public RegisterRequestTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public RegisterRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public RegisterRequestTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public RegisterRequestTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public RegisterRequestTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public RegisterRequest build() {
        return RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}


// ============================================================================
// CREATE USER REQUEST TEST BUILDER
// ============================================================================
public class CreateUserRequestTestBuilder {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserRole role;

    private CreateUserRequestTestBuilder() {
        this.username = "newuser";
        this.email = "newuser@example.com";
        this.password = "password_123";
        this.firstName = "New";
        this.lastName = "User";
        this.role = UserRole.USER;
    }

    public static CreateUserRequestTestBuilder aCreateUserRequest() {
        return new CreateUserRequestTestBuilder();
    }

    public CreateUserRequestTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public CreateUserRequestTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CreateUserRequestTestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public CreateUserRequestTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public CreateUserRequestTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public CreateUserRequestTestBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public CreateUserRequest build() {
        return CreateUserRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .build();
    }
}


// ============================================================================
// UPDATE USER REQUEST TEST BUILDER
// ============================================================================
public class UpdateUserRequestTestBuilder {
    private String firstName;
    private String lastName;
    private UserStatus status;
    private String phone;
    private String department;

    private UpdateUserRequestTestBuilder() {
        this.firstName = "Updated";
        this.lastName = "Name";
        this.status = UserStatus.ACTIVE;
    }

    public static UpdateUserRequestTestBuilder anUpdateUserRequest() {
        return new UpdateUserRequestTestBuilder();
    }

    public UpdateUserRequestTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UpdateUserRequestTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UpdateUserRequestTestBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UpdateUserRequestTestBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public UpdateUserRequestTestBuilder withDepartment(String department) {
        this.department = department;
        return this;
    }

    public UpdateUserRequest build() {
        return UpdateUserRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .status(status)
                .phone(phone)
                .department(department)
                .build();
    }
}


// ============================================================================
// USER RESPONSE TEST BUILDER
// ============================================================================
public class UserResponseTestBuilder {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime joinedDate;

    private UserResponseTestBuilder() {
        this.id = UUID.randomUUID();
        this.username = "testuser";
        this.email = "test@example.com";
        this.firstName = "Test";
        this.lastName = "User";
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.joinedDate = LocalDateTime.now();
    }

    public static UserResponseTestBuilder aUserResponse() {
        return new UserResponseTestBuilder();
    }

    public UserResponseTestBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserResponseTestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserResponseTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserResponseTestBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserResponseTestBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserResponseTestBuilder withRole(UserRole role) {
        this.role = role;
        return this;
    }

    public UserResponseTestBuilder withStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public UserResponseTestBuilder withJoinedDate(LocalDateTime joinedDate) {
        this.joinedDate = joinedDate;
        return this;
    }

    public UserResponse build() {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(status)
                .joinedDate(joinedDate)
                .build();
    }
}


// ============================================================================
// USAGE EXAMPLES IN TESTS:
// ============================================================================
/*
// Simple: Create a standard user
User user = UserTestBuilder.aUser().build();

// With modifications: Create an admin
User admin = UserTestBuilder.anAdmin().withEmail("admin@example.com").build();

// Create expired token
RefreshToken expiredToken = RefreshTokenTestBuilder.anExpiredRefreshToken()
    .withUser(user)
    .build();

// Create valid login request
LoginRequest request = LoginRequestTestBuilder.aLoginRequest()
    .withEmail("test@example.com")
    .withPassword("my_password")
    .build();

// Create update request
UpdateUserRequest updateRequest = UpdateUserRequestTestBuilder.anUpdateUserRequest()
    .withFirstName("Jane")
    .withStatus(UserStatus.INACTIVE)
    .build();

// Usage in test method:
@Test
void testSomething() {
    User testUser = UserTestBuilder.aUser()
        .withEmail("john@example.com")
        .withRole(UserRole.MANAGER)
        .build();
    
    // ... rest of test
}
*/


