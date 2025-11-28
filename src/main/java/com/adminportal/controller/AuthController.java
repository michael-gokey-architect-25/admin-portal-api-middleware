// admin-portal-api/src/main/java/com/adminportal/controller/AuthController.java


// ============================================================================
// PURPOSE: REST Controller for authentication endpoints
// - User login
// - User registration
// - Token refresh
// - User logout
// KEY CONCEPTS:
// 1. @RestController: Combines @Controller + @ResponseBody (returns JSON)
// 2. @RequestMapping: Base path for all endpoints (/auth)
// 3. @PostMapping: HTTP POST method
// 4. @Valid: JSR-303 validation on request body
// 5. @RequestBody: Converts JSON to Java object
// 6. @RequestHeader: Extract headers (e.g., Authorization)
// 7. ResponseEntity: HTTP response with status code
// TEACHING NOTES:
// - Controllers should be thin (logic in services)
// - Controllers handle HTTP details (status codes, headers)
// - Services handle business logic
// - Always validate input with @Valid
// - Return appropriate HTTP status codes (201 for created, 400 for error)
// - Use descriptive method names
// ============================================================================

package com.adminportal.controller;

import com.adminportal.dto.request.LoginRequest;
import com.adminportal.dto.request.RefreshTokenRequest;
import com.adminportal.dto.request.RegisterRequest;
import com.adminportal.dto.response.LoginResponse;
import com.adminportal.dto.response.TokenResponse;
import com.adminportal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication Controller, ENDPOINTS:
 * POST   /auth/login           - User login
 * POST   /auth/register        - User registration
 * POST   /auth/refresh         - Refresh JWT token
 * POST   /auth/logout          - User logout
 * BASE URL: /api/auth (configured in application.yml)
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    // ==================== DEPENDENCIES ====================
    private final AuthService authService;


 
    /** ==================== LOGIN =================
     * POST /auth/login
     * User login with email and password
     *
     * REQUEST:
     * {
     *   "email": "alice@example.com",
     *   "password": "SecureP@ss1"
     * }
     *
     * RESPONSE (200 OK):
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIs...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
     *   "expiresIn": 3600000,
     *   "user": {
     *     "id": "550e8400-...",
     *     "email": "alice@example.com",
     *     "firstName": "Alice",
     *     "lastName": "Admin",
     *     "role": "ADMIN",
     *     "status": "ACTIVE"
     *   }
     * }
     *
     * ERROR (400 Bad Request):
     * {
     *   "code": "AUTHENTICATION_ERROR",
     *   "message": "Invalid email or password",
     *   "timestamp": "2024-01-15T10:30:00Z"
     * }
     *
     * FLOW:
     * 1. Client sends email + password
     * 2. @Valid validates input (email format, required fields)
     * 3. AuthService.login() validates credentials
     * 4. Generate JWT tokens
     * 5. Return tokens + user data
     * 6. Client stores tokens for future requests
     *
     * @param loginRequest Email and password
     * @return ResponseEntity with status 200 and LoginResponse      */
    @PostMapping("/login")
    @Operation(
        summary = "User Login",
        description = "Authenticate user with email and password. Returns JWT tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid email or password"
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



     
    /** ================= DELETE USER =================
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
     * @param id User UUID to delete
     * @return ResponseEntity with 200 status      */
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

        // ================== CALL SERVICE ==================
        userService.deleteUser(id);

        // ================== RETURN 200 OK =================
        return ResponseEntity.ok("User deleted successfully");
    }





     
    /** ================= BULK DELETE USERS =================
     * POST /users/bulk-delete
     * Delete multiple users at once (admin only)
     * REQUEST BODY:
     * {
     *   "userIds": [
     *     "550e8400-e29b-41d4-a716-446655440001",
     *     "550e8400-e29b-41d4-a716-446655440002",
     *     "550e8400-e29b-41d4-a716-446655440003"
     *   ]
     * }
     * RESPONSE (200 OK):
     * {
     *   "deletedCount": 2,
     *   "failedCount": 1,
     *   "message": "2 users deleted successfully, 1 failed"
     * }
     * AUTHORIZATION:
     * - Required: Authenticated user
     * - Required: ADMIN role
     * FLOW:
     * 1. Admin provides list of user IDs
     * 2. Service deletes each user in transaction
     * 3. Track successes and failures
     * 4. Return summary
     * @param bulkDeleteRequest List of user IDs
     * @return ResponseEntity with BulkDeleteResponse      */
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

        // ============= CALL SERVICE ==================
        BulkDeleteResponse response = userService.bulkDeleteUsers(bulkDeleteRequest.userIds());

        // ================ RETURN 200 OK ==================
        return ResponseEntity.ok(response);
    }
}

