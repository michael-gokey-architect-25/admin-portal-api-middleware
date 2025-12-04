// src/test/java/com/adminportal/controller/UserControllerTest.java

// ============================================================================
// Testing Strategy:
// - Use @WebMvcTest to load only controller & security layer
// - Mock service layer dependencies
// - Test HTTP requests/responses via MockMvc
// - Verify status codes, response bodies, pagination, filtering
// - Test authorization with @WithMockUser
// - Test RBAC (role-based access control)
// ============================================================================

package com.adminportal.controller;

import com.adminportal.dto.request.BulkDeleteRequest;
import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.response.BulkDeleteResponse;
import com.adminportal.dto.response.UserListResponse;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.exception.AuthorizationException;
import com.adminportal.exception.DuplicateResourceException;
import com.adminportal.exception.ResourceNotFoundException;
import com.adminportal.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("UserController Unit Tests")
@WebMvcTest(UserController.class)
class UserControllerTest {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private UserResponse userResponse1;
    private UserResponse userResponse2;
    private UserResponse adminResponse;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;
    private BulkDeleteRequest bulkDeleteRequest;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        // Create user response 1
        userResponse1 = UserResponse.builder()
                .id(userId1)
                .username("john_doe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .joinedDate(LocalDateTime.now().minusDays(30))
                .build();

        // Create user response 2
        userResponse2 = UserResponse.builder()
                .id(userId2)
                .username("jane_smith")
                .email("jane@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .joinedDate(LocalDateTime.now().minusDays(15))
                .build();

        // Create admin response
        adminResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .joinedDate(LocalDateTime.now().minusDays(90))
                .build();

        // Create user request
        createUserRequest = CreateUserRequest.builder()
                .username("new_user")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .build();

        // Create update request
        updateUserRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .status(UserStatus.INACTIVE)
                .build();

        // Create bulk delete request
        bulkDeleteRequest = BulkDeleteRequest.builder()
                .userIds(List.of(userId1, userId2))
                .build();
    }


    // ========================================================================
    // GET ALL USERS TESTS - GET /users
    // ========================================================================
    @Test
    @DisplayName("GET ALL USERS - Should return paginated list of users (admin only)")
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_Success() throws Exception {
        // Arrange
        UserListResponse listResponse = UserListResponse.builder()
                .users(List.of(userResponse1, userResponse2))
                .build();

        when(userService.getAllUsers(any(Pageable.class), isNull(), isNull()))
                .thenReturn(listResponse);

        // Act & Assert
        mockMvc.perform(get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(2)))
                .andExpect(jsonPath("$.users[0].username").value("john_doe"))
                .andExpect(jsonPath("$.users[1].username").value("jane_smith"));

        verify(userService).getAllUsers(any(Pageable.class), isNull(), isNull());
    }


    @Test
    @DisplayName("GET ALL USERS - Should return 403 Forbidden when non-admin user")
    @WithMockUser(roles = "USER")
    void testGetAllUsers_Forbidden_NonAdmin() throws Exception {
        // Arrange - No setup needed, authorization check happens in controller

        // Act & Assert
        mockMvc.perform(get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers(any(Pageable.class), anyString(), anyString());
    }


    @Test
    @DisplayName("GET ALL USERS - Should filter by role (admin only)")
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_FilterByRole() throws Exception {
        // Arrange
        UserListResponse listResponse = UserListResponse.builder()
                .users(List.of(userResponse2)) // Only managers
                .build();

        when(userService.getAllUsers(any(Pageable.class), eq("MANAGER"), isNull()))
                .thenReturn(listResponse);

        // Act & Assert
        mockMvc.perform(get("/users")
                .param("role", "MANAGER")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0].role").value("MANAGER"));

        verify(userService).getAllUsers(any(Pageable.class), eq("MANAGER"), isNull());
    }


    @Test
    @DisplayName("GET ALL USERS - Should filter by status (admin only)")
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_FilterByStatus() throws Exception {
        // Arrange
        UserListResponse listResponse = UserListResponse.builder()
                .users(List.of(userResponse1, userResponse2))
                .build();

        when(userService.getAllUsers(any(Pageable.class), isNull(), eq("ACTIVE")))
                .thenReturn(listResponse);

        // Act & Assert
        mockMvc.perform(get("/users")
                .param("status", "ACTIVE")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].status").value(List.of("ACTIVE", "ACTIVE")));

        verify(userService).getAllUsers(any(Pageable.class), isNull(), eq("ACTIVE"));
    }


    @Test
    @DisplayName("GET ALL USERS - Should support pagination")
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers_WithPagination() throws Exception {
        // Arrange
        UserListResponse listResponse = UserListResponse.builder()
                .users(List.of(userResponse1))
                .build();

        when(userService.getAllUsers(any(Pageable.class), isNull(), isNull()))
                .thenReturn(listResponse);

        // Act & Assert
        mockMvc.perform(get("/users")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService).getAllUsers(any(Pageable.class), isNull(), isNull());
    }


    // ========================================================================
    // GET USER BY ID TESTS - GET /users/{id}
    // ========================================================================
    @Test
    @DisplayName("GET USER BY ID - Should return user when found")
    @WithMockUser(roles = "USER")
    void testGetUserById_Success() throws Exception {
        // Arrange
        when(userService.getUserById(userId1))
                .thenReturn(userResponse1);

        // Act & Assert
        mockMvc.perform(get("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId1.toString()))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).getUserById(userId1);
    }


    @Test
    @DisplayName("GET USER BY ID - Should return 404 when user not found")
    @WithMockUser(roles = "USER")
    void testGetUserById_NotFound() throws Exception {
        // Arrange
        when(userService.getUserById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("User", "id", userId1));

        // Act & Assert
        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verify(userService).getUserById(any(UUID.class));
    }


    @Test
    @DisplayName("GET USER BY ID - Should return 401 when not authenticated")
    void testGetUserById_Unauthorized() throws Exception {
        // Act & Assert - No @WithMockUser means not authenticated
        mockMvc.perform(get("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    // ========================================================================
    // GET USER PROFILE TESTS - GET /users/{id}/profile
    // ========================================================================
    @Test
    @DisplayName("GET USER PROFILE - Should return user profile with permissions")
    @WithMockUser(roles = "USER")
    void testGetUserProfile_Success() throws Exception {
        // Arrange
        when(userService.getUserProfile(userId1))
                .thenReturn(userResponse1);

        // Act & Assert
        mockMvc.perform(get("/users/{id}/profile", userId1)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).getUserProfile(userId1);
    }


    // ========================================================================
    // SEARCH USERS TESTS - GET /users/search
    // ========================================================================
    @Test
    @DisplayName("SEARCH USERS - Should find users by search term")
    @WithMockUser(roles = "USER")
    void testSearchUsers_Found() throws Exception {
        // Arrange
        UserListResponse searchResponse = UserListResponse.builder()
                .users(List.of(userResponse1))
                .build();

        when(userService.searchUsers(eq("john"), any(Pageable.class)))
                .thenReturn(searchResponse);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                .param("term", "john")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0].username").value("john_doe"));

        verify(userService).searchUsers(eq("john"), any(Pageable.class));
    }


    @Test
    @DisplayName("SEARCH USERS - Should return empty list when no results")
    @WithMockUser(roles = "USER")
    void testSearchUsers_NoResults() throws Exception {
        // Arrange
        UserListResponse emptyResponse = UserListResponse.builder()
                .users(List.of())
                .build();

        when(userService.searchUsers(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(emptyResponse);

        // Act & Assert
        mockMvc.perform(get("/users/search")
                .param("term", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(0)));

        verify(userService).searchUsers(eq("nonexistent"), any(Pageable.class));
    }


    @Test
    @DisplayName("SEARCH USERS - Should return 400 when search term is missing")
    @WithMockUser(roles = "USER")
    void testSearchUsers_MissingTerm() throws Exception {
        // Act & Assert - Missing 'term' parameter
        mockMvc.perform(get("/users/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).searchUsers(anyString(), any(Pageable.class));
    }


    // ========================================================================
    // CREATE USER TESTS - POST /users
    // ========================================================================
    @Test
    @DisplayName("CREATE USER - Should create user successfully (admin only)")
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_Success() throws Exception {
        // Arrange
        UserResponse newUserResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("new_user")
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .joinedDate(LocalDateTime.now())
                .build();

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(newUserResponse);

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.email").value("new@example.com"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }


    @Test
    @DisplayName("CREATE USER - Should return 403 when non-admin user tries to create")
    @WithMockUser(roles = "USER")
    void testCreateUser_Forbidden_NonAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }


    @Test
    @DisplayName("CREATE USER - Should return 409 when email already exists")
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_DuplicateEmail() throws Exception {
        // Arrange
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "new@example.com"));

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }


    @Test
    @DisplayName("CREATE USER - Should return 400 when request is invalid")
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_InvalidRequest() throws Exception {
        // Arrange - Invalid email
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .username("new_user")
                .email("invalid_email")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }


    // ========================================================================
    // UPDATE USER TESTS - PUT /users/{id}
    // ========================================================================
    @Test
    @DisplayName("UPDATE USER - Admin can update any user")
    @WithMockUser(roles = "ADMIN")
    void testUpdateUser_AdminUpdatingOther() throws Exception {
        // Arrange
        UserResponse updatedUser = UserResponse.builder()
                .id(userId1)
                .username("john_doe")
                .email("john@example.com")
                .firstName("Updated")
                .lastName("Name")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .joinedDate(LocalDateTime.now())
                .build();

        when(userService.updateUser(eq(userId1), any(UpdateUserRequest.class), any()))
                .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(userService).updateUser(eq(userId1), any(UpdateUserRequest.class), any());
    }


    @Test
    @DisplayName("UPDATE USER - User can only update their own profile")
    @WithMockUser(username = "john_doe", roles = "USER")
    void testUpdateUser_UserUpdatingSelf() throws Exception {
        // Arrange
        when(userService.updateUser(eq(userId1), any(UpdateUserRequest.class), any()))
                .thenReturn(userResponse1);

        // Act & Assert
        mockMvc.perform(put("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(userId1), any(UpdateUserRequest.class), any());
    }


    @Test
    @DisplayName("UPDATE USER - Should return 403 when user tries to update another user")
    @WithMockUser(username = "john_doe", roles = "USER")
    void testUpdateUser_UserUpdatingOther() throws Exception {
        // Arrange
        when(userService.updateUser(eq(userId2), any(UpdateUserRequest.class), any()))
                .thenThrow(new AuthorizationException("ADMIN", null));

        // Act & Assert
        mockMvc.perform(put("/users/{id}", userId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(userService).updateUser(eq(userId2), any(UpdateUserRequest.class), any());
    }


    @Test
    @DisplayName("UPDATE USER - Should return 404 when user not found")
    @WithMockUser(roles = "ADMIN")
    void testUpdateUser_NotFound() throws Exception {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        when(userService.updateUser(eq(nonexistentId), any(UpdateUserRequest.class), any()))
                .thenThrow(new ResourceNotFoundException("User", "id", nonexistentId));

        // Act & Assert
        mockMvc.perform(put("/users/{id}", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateUserRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }


    // ========================================================================
    // DELETE USER TESTS - DELETE /users/{id}
    // ========================================================================
    @Test
    @DisplayName("DELETE USER - Admin can delete any user")
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_AdminDeleting() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(eq(userId1), any());

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService).deleteUser(eq(userId1), any());
    }


    @Test
    @DisplayName("DELETE USER - Should return 403 when non-admin tries to delete")
    @WithMockUser(roles = "USER")
    void testDeleteUser_Forbidden_NonAdmin() throws Exception {
        // Arrange
        doThrow(new AuthorizationException("ADMIN", null))
                .when(userService).deleteUser(eq(userId1), any());

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", userId1)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService).deleteUser(eq(userId1), any());
    }


    @Test
    @DisplayName("DELETE USER - Should return 404 when user not found")
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser_NotFound() throws Exception {
        // Arrange
        UUID nonexistentId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("User", "id", nonexistentId))
                .when(userService).deleteUser(eq(nonexistentId), any());

        // Act & Assert
        mockMvc.perform(delete("/users/{id}", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }


    // ========================================================================
    // BULK DELETE TESTS - POST /users/bulk-delete
    // ========================================================================
    @Test
    @DisplayName("BULK DELETE - Should delete multiple users (admin only)")
    @WithMockUser(roles = "ADMIN")
    void testBulkDeleteUsers_Success() throws Exception {
        // Arrange
        BulkDeleteResponse response = BulkDeleteResponse.builder()
                .successCount(2)
                .failureCount(0)
                .build();

        when(userService.bulkDeleteUsers(any(BulkDeleteRequest.class), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/users/bulk-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDeleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0));

        verify(userService).bulkDeleteUsers(any(BulkDeleteRequest.class), any());
    }


    @Test
    @DisplayName("BULK DELETE - Should return 403 when non-admin tries to delete")
    @WithMockUser(roles = "USER")
    void testBulkDeleteUsers_Forbidden_NonAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/users/bulk-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDeleteRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).bulkDeleteUsers(any(BulkDeleteRequest.class), any());
    }


    @Test
    @DisplayName("BULK DELETE - Should handle partial failures")
    @WithMockUser(roles = "ADMIN")
    void testBulkDeleteUsers_PartialFailure() throws Exception {
        // Arrange
        BulkDeleteResponse response = BulkDeleteResponse.builder()
                .successCount(1)
                .failureCount(1)
                .build();

        when(userService.bulkDeleteUsers(any(BulkDeleteRequest.class), any()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/users/bulk-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkDeleteRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1));
    }


    @Test
    @DisplayName("BULK DELETE - Should return 400 when list is empty")
    @WithMockUser(roles = "ADMIN")
    void testBulkDeleteUsers_EmptyList() throws Exception {
        // Arrange
        BulkDeleteRequest emptyRequest = BulkDeleteRequest.builder()
                .userIds(List.of())
                .build();

        // Act & Assert
        mockMvc.perform(post("/users/bulk-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    // ========================================================================
    // CONTENT TYPE & HEADER TESTS
    // ========================================================================
    @Test
    @DisplayName("GET USERS - Should return JSON content type")
    @WithMockUser(roles = "ADMIN")
    void testGetUsers_ResponseContentType() throws Exception {
        // Arrange
        UserListResponse listResponse = UserListResponse.builder()
                .users(List.of(userResponse1))
                .build();

        when(userService.getAllUsers(any(Pageable.class), isNull(), isNull()))
                .thenReturn(listResponse);

        // Act & Assert
        mockMvc.perform(get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }


    @Test
    @DisplayName("CREATE USER - Should return 201 Created status")
    @WithMockUser(roles = "ADMIN")
    void testCreateUser_ResponseStatus() throws Exception {
        // Arrange
        UserResponse newUserResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("new_user")
                .email("new@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(newUserResponse);

        // Act & Assert
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated());
    }

    /*  ========================================================================
    // TEST EXECUTION TIPS:
    Run individual test:
        mvn test -Dtest=UserControllerTest#testCreateUser_Success
    Run all UserController tests:
        mvn test -Dtest=UserControllerTest
    Run with detailed output:
        mvn test -Dtest=UserControllerTest -X
    Run specific test pattern:
        mvn test -Dtest=UserControllerTest#test*Delete*
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0   */
}


