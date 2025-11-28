// admin-portal-api/src/main/java/com/adminportal/dto/request/CreateUserRequest.java

// ============================================================================
// PURPOSE: Request DTO for admin user creation endpoint
// - Used only by ADMIN role
// - Includes all user fields plus role assignment
// - Cannot use records for this complex object (requires custom constructor)
// ============================================================================

package com.adminportal.dto.request;

import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create new user (admin only)")
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    @Schema(example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    @Schema(example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(example = "john@example.com")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20)
    @Schema(example = "john.doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8)
    @Schema(example = "SecureP@ss1")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(example = "USER", description = "USER, MANAGER, or ADMIN")
    private UserRole role;

    @Schema(example = "ACTIVE", description = "ACTIVE, INACTIVE, or SUSPENDED")
    private UserStatus status = UserStatus.ACTIVE;

    @Schema(example = "+1-555-0100")
    private String phone;

    @Schema(example = "Engineering")
    private String department;

    @Schema(example = "false")
    private Boolean canManageUsers = false;

    @Schema(example = "false")
    private Boolean canViewReports = false;

    @Schema(example = "false")
    private Boolean canManageSettings = false;
}

