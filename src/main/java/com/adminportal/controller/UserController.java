// admin-portal-api/src/main/java/com/adminportal/controller/UserController.java

// ============================================================================
// PURPOSE: REST Controller for user management endpoints
// - List all users (paginated, filtered)
// - Get single user
// - Create user (admin)
// - Update user
// - Delete user (admin)
// - Search users
// - Bulk delete (admin)
// KEY CONCEPTS:
// 1. @GetMapping: HTTP GET (retrieve data)
// 2. @PostMapping: HTTP POST (create data)
// 3. @PutMapping: HTTP PUT (update data)
// 4. @DeleteMapping: HTTP DELETE (delete data)
// 5. @PathVariable: Extract ID from URL path (/users/{id})
// 6. @RequestParam: Extract query parameters (?page=1&limit=10)
// 7. @PreAuthorize: Method-level authorization (Spring Security)
// TEACHING NOTES:
// - Controllers are thin - most logic in services
// - Controllers handle HTTP details (status codes, headers)
// - Always validate input with @Valid
// - Return appropriate HTTP status codes
// - Log important operations for debugging
// ============================================================================

package com.adminportal.controller;

import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.BulkDeleteRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.response.BulkDeleteResponse;
import com.adminportal.dto.response.UserListResponse;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


/**
 * User Management Controller, ENDPOINTS:
 * GET    /users                       - List all users (admin only)
 * GET    /users/{id}                  - Get single user
 * GET    /users/{id}/profile          - Get user profile with permissions
 * GET    /users/search                - Search users
 * POST   /users                       - Create user (admin only)
 * PUT    /users/{id}                  - Update user
 * DELETE /users/{id}                  - Delete user (admin only)
 * POST   /users/bulk-delete           - Delete multiple users (admin only)
 * BASE URL: /api/users
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints (CRUD operations)")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    // ==================== DEPENDENCIES ====================
    private final UserService userService;

    // ==================== GET ALL USERS (PAGINATED/FILTERED) ====================
    /**
     * GET /users?page=1&limit=10&role=MANAGER&sortBy=name&sortOrder=asc
     * List all users (admin only) with pagination, filtering, sorting
     *
     * QUERY PARAMETERS:
     * - page: Page number (1-indexed, default 1)
     * - limit: Items per page (default 10, max 100)
     * - role: Filter by role (ADMIN, MANAGER, USER) - optional
     * - status: Filter by status (ACTIVE, INACTIVE, SUSPENDED) - optional
     * - sortBy: Sort field (name, email, role, joinedDate) - default name
     * - sortOrder: Sort direction (asc, desc) - default asc
     *
     * RESPONSE (200 OK):
     * {
     *   "data": [
     *     { user1 }, { user2 }, ...
     *   ],
     *   "pagination": {
     *     "page": 1,
     *     "limit": 10,
     *     "total": 50,
     *     "totalPages": 5
     *   }
     * }
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Required: ADMIN role
     *
     * FLOW:
     * 1. @PreAuthorize checks if user has ADMIN role
     * 2. If yes, proceed; if no, return 403 Forbidden
     * 3. UserService.getAllUsers() executes with filters
     * 4. Return 200 OK with paginated results
     *
     * @param page Page number (1-indexed)
     * @param limit Items per page
     * @param role Optional role filter
     * @param status Optional status filter
     * @param sortBy Field to sort (default name)
     * @param sortOrder Sort direction (default asc)
     * @return ResponseEntity with UserListResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can list all users
    @Operation(
        summary = "List All Users",
        description = "Retrieve paginated list of all users (Admin only)"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserListResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required"
        )
    })
    public ResponseEntity<UserListResponse> getAllUsers(
            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "10") int limit,

            @Parameter(description = "Filter by role")
            @RequestParam(required = false) String role,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) String status,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "asc") String sortOrder) {

        log.info("Fetching users - page: {}, limit: {}, role: {}, status: {}, sortBy: {}, sortOrder: {}",
            page, limit, role, status, sortBy, sortOrder);

        // ==================== CALL SERVICE ====================
        UserListResponse response = userService.getAllUsers(page, limit, role, status, sortBy, sortOrder);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(response);
    }




    /** ==== GET SINGLE USER =========
     * GET /users/{id}
     * Get single user by ID
     *
     * PATH PARAMETERS:
     * - id: User UUID
     *
     * RESPONSE (200 OK):
     * {
     *   "id": "550e8400-...",
     *   "email": "alice@example.com",
     *   "firstName": "Alice",
     *   "lastName": "Admin",
     *   "role": "ADMIN",
     *   "status": "ACTIVE",
     *   ...
     * }
     *
     * ERROR (404 Not Found):
     * {
     *   "code": "RESOURCE_NOT_FOUND",
     *   "message": "User not found",
     *   "timestamp": "2024-01-15T10:30:00Z"
     * }
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - No role check (users can see other users)
     *
     * @param userId User UUID from path
     * @return ResponseEntity with UserResponse
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get User by ID",
        description = "Retrieve a specific user by ID"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User UUID")
            @PathVariable UUID id) {

        log.debug("Fetching user: {}", id);

        // ==================== CALL SERVICE ====================
        UserResponse response = userService.getUserById(id);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(response);
    }




    /** ======== GET USER PROFILE ===============
     * GET /users/{id}/profile
     * Get user profile with permissions
     *
     * RESPONSE (200 OK):
     * {
     *   "id": "550e8400-...",
     *   "email": "alice@example.com",
     *   "role": "ADMIN",
     *   "permissions": ["view_dashboard", "manage_users", ...],
     *   ...
     * }
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Can access own profile or if admin
     *
     * @param id User UUID
     * @return ResponseEntity with UserResponse
     */
    @GetMapping("/{id}/profile")
    @Operation(
        summary = "Get User Profile",
        description = "Get current user's profile or admin can get any user's profile"
    )
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<UserResponse> getUserProfile(
            @Parameter(description = "User UUID")
            @PathVariable UUID id) {

        log.debug("Fetching user profile: {}", id);

        // ==================== CALL SERVICE ====================
        UserResponse response = userService.getUserProfile(id);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(response);
    }




    /** ======== SEARCH USERS ========
     * GET /users/search?term=alice&limit=20
     * Search users by name, email, or username
     *
     * QUERY PARAMETERS:
     * - term: Search term (required)
     * - limit: Max results (default 20, max 100)
     *
     * RESPONSE (200 OK):
     * [
     *   { user1 },
     *   { user2 },
     *   ...
     * ]
     *
     * FLOW:
     * 1. User provides search term
     * 2. Service queries database with LIKE search
     * 3. Case-insensitive match on first name, last name, email, username
     * 4. Return matching users
     *
     * @param term Search text (required)
     * @param limit Max results
     * @return ResponseEntity with List of UserResponse
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search Users",
        description = "Search users by name, email, or username"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search results"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Search term", required = true)
            @RequestParam String term,

            @Parameter(description = "Max results")
            @RequestParam(defaultValue = "20") int limit) {

        log.debug("Searching users with term: {}", term);

        // ==================== CALL SERVICE ====================
        List<UserResponse> results = userService.searchUsers(term, limit);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(results);
    }



   
    /** ============= CREATE USER ========
     * POST /users
     * Create new user (admin only)
     *
     * REQUEST BODY:
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com",
     *   "username": "john.doe",
     *   "password": "SecureP@ss1",
     *   "role": "USER",
     *   "status": "ACTIVE",
     *   "phone": "+1-555-0100",
     *   "department": "Engineering",
     *   "canManageUsers": false,
     *   "canViewReports": false,
     *   "canManageSettings": false
     * }
     *
     * RESPONSE (201 Created):
     * {
     *   "id": "550e8400-...",
     *   "email": "john@example.com",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "role": "USER",
     *   "status": "ACTIVE",
     *   ...
     * }
     *
     * ERROR (409 Conflict):
     * {
     *   "code": "DUPLICATE_RESOURCE",
     *   "message": "Email already registered",
     *   "timestamp": "2024-01-15T10:30:00Z"
     * }
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Required: ADMIN role
     *
     * @param createUserRequest User data
     * @return ResponseEntity with status 201 and UserResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can create users
    @Operation(
        summary = "Create User",
        description = "Create a new user (Admin only)"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email or username already exists"
        )
    })
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest createUserRequest) {

        log.info("Creating new user: {}", createUserRequest.getEmail());

        // =================== CALL SERVICE ====================
        UserResponse response = userService.createUser(createUserRequest);

        // ================= RETURN 201 CREATED ====================
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }




   
    /** ========== UPDATE USER ===========
     * PUT /users/{id}
     * Update user information
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Can update own profile
     * - Admin can update any profile and change roles
     *
     * REQUEST BODY (all optional):
     * {
     *   "firstName": "Alice",
     *   "lastName": "Admin",
     *   "email": "alice@example.com",
     *   "password": "NewSecureP@ss1",
     *   "phone": "+1-555-0100",
     *   "department": "Engineering",
     *   "role": "ADMIN",  // Admin only
     *   "status": "ACTIVE",  // Admin only
     *   "canManageUsers": true,  // Admin only
     *   "canViewReports": true,  // Admin only
     *   "canManageSettings": true  // Admin only
     * }
     *
     * RESPONSE (200 OK):
     * {
     *   "id": "550e8400-...",
     *   "email": "alice@example.com",
     *   ...
     * }
     *
     * ERROR (403 Forbidden):
     * {
     *   "code": "FORBIDDEN",
     *   "message": "Cannot update other user's profile",
     *   "timestamp": "2024-01-15T10:30:00Z"
     * }
     *
     * @param id User UUID to update
     * @param updateUserRequest Fields to update
     * @return ResponseEntity with updated UserResponse
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update User",
        description = "Update user information (Admin only or own profile)"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email already exists"
        )
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User UUID to update")
            @PathVariable UUID id,

            @Valid @RequestBody UpdateUserRequest updateUserRequest) {

        log.info("Updating user: {}", id);

        // ==================== GET CURRENT USER ====================
        // Extract from Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID currentUserId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // ==================== CALL SERVICE ====================
        UserResponse response = userService.updateUser(id, updateUserRequest, currentUserId, isAdmin);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(response);
    }





    /** ============= DELETE USER ==========
     * DELETE /users/{id}
     * Delete user (admin only)
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Required: ADMIN role
     *
     * RESPONSE (200 OK):
     * {
     *   "message": "User deleted successfully"
     * }
     *
     * DELETES:
     * - User record
     * - All refresh tokens (cascade delete)
     * - User data cannot be recovered
     *
     * @param id User UUID to delete
     * @return ResponseEntity with 200 status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can delete users
    @Operation(
        summary = "Delete User",
        description = "Delete a user by ID (Admin only)"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User deleted successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "User UUID to delete")
            @PathVariable UUID id) {

        log.info("Deleting user: {}", id);

        // ==================== CALL SERVICE ====================
        userService.deleteUser(id);

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok("User deleted successfully");
    }






    /** ========= BULK DELETE USERS ========
     * POST /users/bulk-delete
     * Delete multiple users at once (admin only)
     *
     * REQUEST BODY:
     * {
     *   "userIds": [
     *     "550e8400-e29b-41d4-a716-446655440001",
     *     "550e8400-e29b-41d4-a716-446655440002",
     *     "550e8400-e29b-41d4-a716-446655440003"
     *   ]
     * }
     *
     * RESPONSE (200 OK):
     * {
     *   "deletedCount": 2,
     *   "failedCount": 1,
     *   "message": "2 users deleted successfully, 1 failed"
     * }
     *
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Required: ADMIN role
     *
     * FLOW:
     * 1. Admin provides list of user IDs
     * 2. Service deletes each user in transaction
     * 3. Track successes and failures
     * 4. Return summary
     *
     * @param bulkDeleteRequest List of user IDs
     * @return ResponseEntity with BulkDeleteResponse
     */
    @PostMapping("/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")  // Only admins can bulk delete
    @Operation(
        summary = "Bulk Delete Users",
        description = "Delete multiple users at once (Admin only)"
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users deleted successfully",
            content = @Content(schema = @Schema(implementation = BulkDeleteResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required"
        )
    })
    public ResponseEntity<BulkDeleteResponse> bulkDeleteUsers(
            @Valid @RequestBody BulkDeleteRequest bulkDeleteRequest) {

        log.info("Bulk deleting {} users", bulkDeleteRequest.userIds().size());

        // ==================== CALL SERVICE ====================
        BulkDeleteResponse response = userService.bulkDeleteUsers(bulkDeleteRequest.userIds());

        // ==================== RETURN 200 OK ====================
        return ResponseEntity.ok(response);
    }
}


