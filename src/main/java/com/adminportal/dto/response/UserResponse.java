// admin-portal-api/src/main/java/com/adminportal/dto/response/UserResponse.java

// ============================================================================
// PURPOSE: Response DTO for user data in API responses
// - Excludes sensitive fields (password)
// - Used in LoginResponse, UserListResponse, and individual user endpoints
// ============================================================================

package com.adminportal.dto.response;

import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User information")
public class UserResponse {

    @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Email", example = "john@example.com")
    private String email;

    @Schema(description = "Username", example = "john.doe")
    private String username;

    @Schema(description = "User role", example = "USER")
    private UserRole role;

    @Schema(description = "Account status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Phone number", example = "+1-555-0100")
    private String phone;

    @Schema(description = "Department", example = "Engineering")
    private String department;

    @Schema(description = "Account created date")
    private LocalDateTime joinedDate;

    @Schema(description = "Last login date")
    private LocalDateTime lastLoginDate;

    @Schema(description = "Can manage users", example = "false")
    private Boolean canManageUsers;

    @Schema(description = "Can view reports", example = "false")
    private Boolean canViewReports;

    @Schema(description = "Can manage settings", example = "false")
    private Boolean canManageSettings;
}

