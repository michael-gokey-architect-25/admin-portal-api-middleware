// src/test/java/com/adminportal/service/UserServiceTest.java

// ============================================================================
// Testing Strategy:
// - Mock UserRepository, PasswordEncoder, UserMapper
// - No database calls (all mocked)
// - Test CRUD operations (Create, Read, Update, Delete)
// - Test pagination, filtering, search
// - Test bulk operations
// - Test authorization checks
// ============================================================================

package com.adminportal.service;

import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.request.BulkDeleteRequest;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.dto.response.UserListResponse;
import com.adminportal.dto.response.BulkDeleteResponse;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.exception.AuthorizationException;
import com.adminportal.exception.DuplicateResourceException;
import com.adminportal.exception.ResourceNotFoundException;
import com.adminportal.mapper.UserMapper;
import com.adminportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserService Unit Tests")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // ========================================================================
    // MOCKED DEPENDENCIES
    // ========================================================================
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private User adminUser;
    private User regularUser;
    private User managerUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        // Admin user for authorization checks
        adminUser = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
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

        // Regular user
        regularUser = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
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

        // Manager user
        managerUser = User.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .username("jane_manager")
                .email("jane@example.com")
                .password("hashed_manager_password")
                .firstName("Jane")
                .lastName("Manager")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // User response DTO
        userResponse = UserResponse.builder()
                .id(regularUser.getId())
                .username(regularUser.getUsername())
                .email(regularUser.getEmail())
                .firstName(regularUser.getFirstName())
                .lastName(regularUser.getLastName())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .joinedDate(regularUser.getCreatedAt())
                .build();
    }


    // ========================================================================
    // GET ALL USERS TESTS
    // ========================================================================
    @Test
    @DisplayName("GET ALL USERS - Should return paginated list of users")
    void testGetAllUsers_Success() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<User> userPage = new PageImpl<>(
                List.of(regularUser, managerUser),
                pageable,
                2
        );

        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        when(userMapper.toResponse(regularUser))
                .thenReturn(userResponse);

        when(userMapper.toResponse(managerUser))
                .thenReturn(UserResponse.builder()
                        .id(managerUser.getId())
                        .username(managerUser.getUsername())
                        .email(managerUser.getEmail())
                        .role(UserRole.MANAGER)
                        .build());

        // Act
        UserListResponse response = userService.getAllUsers(pageable, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getUsers().size());
        assertEquals(2, response.getPagination().getTotalElements());
        assertEquals(1, response.getPagination().getTotalPages());

        verify(userRepository, times(1)).findAll(pageable);
        verify(userMapper, times(2)).toResponse(any(User.class));
    }


    @Test
    @DisplayName("GET ALL USERS - Should filter by role")
    void testGetAllUsers_FilterByRole() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<User> userPage = new PageImpl<>(
                List.of(adminUser),
                pageable,
                1
        );

        when(userRepository.findByRole(UserRole.ADMIN, pageable))
                .thenReturn(userPage);

        when(userMapper.toResponse(adminUser))
                .thenReturn(UserResponse.builder()
                        .id(adminUser.getId())
                        .role(UserRole.ADMIN)
                        .build());

        // Act
        UserListResponse response = userService.getAllUsers(pageable, UserRole.ADMIN.name(), null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getUsers().size());

        verify(userRepository, times(1)).findByRole(UserRole.ADMIN, pageable);
    }


    @Test
    @DisplayName("GET ALL USERS - Should filter by status")
    void testGetAllUsers_FilterByStatus() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<User> userPage = new PageImpl<>(
                List.of(regularUser),
                pageable,
                1
        );

        when(userRepository.findByStatus(UserStatus.ACTIVE, pageable))
                .thenReturn(userPage);

        when(userMapper.toResponse(regularUser))
                .thenReturn(userResponse);

        // Act
        UserListResponse response = userService.getAllUsers(pageable, null, UserStatus.ACTIVE.name());

        // Assert
        assertEquals(1, response.getUsers().size());
        verify(userRepository, times(1)).findByStatus(UserStatus.ACTIVE, pageable);
    }


    // ========================================================================
    // GET USER BY ID TESTS
    // ========================================================================
    @Test
    @DisplayName("GET USER BY ID - Should return user when found")
    void testGetUserById_Found() {
        // Arrange
        UUID userId = regularUser.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        when(userMapper.toResponse(regularUser))
                .thenReturn(userResponse);

        // Act
        UserResponse response = userService.getUserById(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("john_doe", response.getUsername());

        verify(userRepository, times(1)).findById(userId);
        verify(userMapper, times(1)).toResponse(regularUser);
    }


    @Test
    @DisplayName("GET USER BY ID - Should throw exception when user not found")
    void testGetUserById_NotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });

        verify(userRepository, times(1)).findById(userId);
    }


    // ========================================================================
    // GET USER PROFILE TESTS
    // ========================================================================
    @Test
    @DisplayName("GET USER PROFILE - Should return user profile with permissions")
    void testGetUserProfile_Success() {
        // Arrange
        UUID userId = regularUser.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        when(userMapper.toResponse(regularUser))
                .thenReturn(userResponse);

        // Act
        UserResponse response = userService.getUserProfile(userId);

        // Assert
        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());

        verify(userRepository, times(1)).findById(userId);
    }


    // ========================================================================
    // SEARCH USERS TESTS
    // ========================================================================
    @Test
    @DisplayName("SEARCH USERS - Should find users by search term")
    void testSearchUsers_Found() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<User> searchResults = new PageImpl<>(
                List.of(regularUser),
                pageable,
                1
        );

        when(userRepository.findBySearchTerm("john", pageable))
                .thenReturn(searchResults);

        when(userMapper.toResponse(regularUser))
                .thenReturn(userResponse);

        // Act
        UserListResponse response = userService.searchUsers("john", pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getUsers().size());
        assertEquals("john_doe", response.getUsers().get(0).getUsername());

        verify(userRepository, times(1)).findBySearchTerm("john", pageable);
    }


    @Test
    @DisplayName("SEARCH USERS - Should return empty list when no matches")
    void testSearchUsers_NoResults() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(userRepository.findBySearchTerm("nonexistent", pageable))
                .thenReturn(emptyPage);

        // Act
        UserListResponse response = userService.searchUsers("nonexistent", pageable);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getUsers().size());
    }


    // ========================================================================
    // CREATE USER TESTS
    // ========================================================================
    @Test
    @DisplayName("CREATE USER - Should create new user successfully")
    void testCreateUser_Success() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("new_user")
                .email("new@example.com")
                .password("new_password_123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .username("new_user")
                .email("new@example.com")
                .password("hashed_new_password")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(false);

        when(userRepository.existsByUsername("new_user"))
                .thenReturn(false);

        when(passwordEncoder.encode("new_password_123"))
                .thenReturn("hashed_new_password");

        when(userRepository.save(any(User.class)))
                .thenReturn(newUser);

        UserResponse newUserResponse = UserResponse.builder()
                .id(newUser.getId())
                .username("new_user")
                .email("new@example.com")
                .build();

        when(userMapper.toResponse(newUser))
                .thenReturn(newUserResponse);

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("new_user", response.getUsername());
        assertEquals("new@example.com", response.getEmail());

        // Verify the user was saved with correct data
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("new_user", savedUser.getUsername());
        assertEquals("hashed_new_password", savedUser.getPassword());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
    }


    @Test
    @DisplayName("CREATE USER - Should throw exception when email exists")
    void testCreateUser_DuplicateEmail() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("new_user")
                .email("john@example.com") // Already exists
                .password("password")
                .build();

        when(userRepository.existsByEmail("john@example.com"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    @DisplayName("CREATE USER - Should throw exception when username exists")
    void testCreateUser_DuplicateUsername() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .username("john_doe") // Already exists
                .email("new@example.com")
                .password("password")
                .build();

        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(false);

        when(userRepository.existsByUsername("john_doe"))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }


    // ========================================================================
    // UPDATE USER TESTS
    // ========================================================================
    @Test
    @DisplayName("UPDATE USER - Admin can update any user")
    void testUpdateUser_AdminUpdatingOtherUser() {
        // Arrange
        UUID userId = regularUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .status(UserStatus.INACTIVE)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        User updatedUser = regularUser;
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setStatus(UserStatus.INACTIVE);

        when(userRepository.save(any(User.class)))
                .thenReturn(updatedUser);

        when(userMapper.toResponse(updatedUser))
                .thenReturn(UserResponse.builder()
                        .id(userId)
                        .firstName("Updated")
                        .lastName("Name")
                        .status(UserStatus.INACTIVE)
                        .build());

        // Act
        UserResponse response = userService.updateUser(userId, request, adminUser);

        // Assert
        assertNotNull(response);
        assertEquals("Updated", response.getFirstName());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    @DisplayName("UPDATE USER - User can only update their own profile")
    void testUpdateUser_UserUpdatingSelf() {
        // Arrange
        UUID userId = regularUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        User updatedUser = regularUser;
        updatedUser.setFirstName("Updated");

        when(userRepository.save(any(User.class)))
                .thenReturn(updatedUser);

        when(userMapper.toResponse(updatedUser))
                .thenReturn(userResponse);

        // Act
        UserResponse response = userService.updateUser(userId, request, regularUser);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    @DisplayName("UPDATE USER - User cannot update other users")
    void testUpdateUser_UserUpdatingOther() {
        // Arrange
        UUID otherId = managerUser.getId();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Hacked")
                .build();

        when(userRepository.findById(otherId))
                .thenReturn(Optional.of(managerUser));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> {
            userService.updateUser(otherId, request, regularUser);
        });

        verify(userRepository, never()).save(any(User.class));
    }


    // ========================================================================
    // DELETE USER TESTS
    // ========================================================================
    @Test
    @DisplayName("DELETE USER - Admin can delete any user")
    void testDeleteUser_AdminDeleting() {
        // Arrange
        UUID userId = regularUser.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        // Act
        userService.deleteUser(userId, adminUser);

        // Assert
        verify(userRepository, times(1)).deleteById(userId);
    }


    @Test
    @DisplayName("DELETE USER - Non-admin cannot delete")
    void testDeleteUser_NonAdminDeleting() {
        // Arrange
        UUID userId = regularUser.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(regularUser));

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> {
            userService.deleteUser(userId, regularUser);
        });

        verify(userRepository, never()).deleteById(any());
    }


    @Test
    @DisplayName("DELETE USER - Should throw exception when user not found")
    void testDeleteUser_NotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(userId, adminUser);
        });

        verify(userRepository, never()).deleteById(any());
    }


    // ========================================================================
    // BULK DELETE TESTS
    // ========================================================================
    @Test
    @DisplayName("BULK DELETE - Should delete multiple users successfully")
    void testBulkDeleteUsers_Success() {
        // Arrange
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        BulkDeleteRequest request = BulkDeleteRequest.builder()
                .userIds(List.of(userId1, userId2))
                .build();

        User user1 = User.builder().id(userId1).build();
        User user2 = User.builder().id(userId2).build();

        when(userRepository.findById(userId1))
                .thenReturn(Optional.of(user1));

        when(userRepository.findById(userId2))
                .thenReturn(Optional.of(user2));

        // Act
        BulkDeleteResponse response = userService.bulkDeleteUsers(request, adminUser);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());

        verify(userRepository, times(2)).deleteById(any());
    }


    @Test
    @DisplayName("BULK DELETE - Should handle partial failures")
    void testBulkDeleteUsers_PartialFailure() {
        // Arrange
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        BulkDeleteRequest request = BulkDeleteRequest.builder()
                .userIds(List.of(userId1, userId2))
                .build();

        User user1 = User.builder().id(userId1).build();

        when(userRepository.findById(userId1))
                .thenReturn(Optional.of(user1));

        when(userRepository.findById(userId2))
                .thenReturn(Optional.empty());

        // Act
        BulkDeleteResponse response = userService.bulkDeleteUsers(request, adminUser);

        // Assert
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
    }


    @Test
    @DisplayName("BULK DELETE - Non-admin cannot bulk delete")
    void testBulkDeleteUsers_NonAdminDeleting() {
        // Arrange
        BulkDeleteRequest request = BulkDeleteRequest.builder()
                .userIds(List.of(UUID.randomUUID()))
                .build();

        // Act & Assert
        assertThrows(AuthorizationException.class, () -> {
            userService.bulkDeleteUsers(request, regularUser);
        });

        verify(userRepository, never()).deleteById(any());
    }
}

/* ========================================================
// TEST EXECUTION TIPS:
1. Run individual test:
   mvn test -Dtest=UserServiceTest#testCreateUser_Success
2. Run all UserService tests:
   mvn test -Dtest=UserServiceTest
3. Run specific test class pattern:
   mvn test -Dtest=UserServiceTest#testCreate*
4. Run with coverage reporting:
   mvn test jacoco:report
5. View coverage report:
   open target/site/jacoco/index.html (on Mac)
   target/site/jacoco/index.html (on Linux/Windows)
6. Expected output:
   [INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
*/


