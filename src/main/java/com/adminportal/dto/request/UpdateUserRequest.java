// admin-portal-api/src/main/java/com/adminportal/dto/request/UpdateUserRequest.java

// ============================================================================
// PURPOSE: Request DTO for updating existing user
// - All fields optional (partial updates)
// - Password update handled securely
// ============================================================================

package com.adminportal.dto.request;

import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user data")
public class UpdateUserRequest {

    @Size(min = 2, max = 50)
    @Schema(example = "John")
    private String firstName;

    @Size(min = 2, max = 50)
    @Schema(example = "Doe")
    private String lastName;

    @Email(message = "Email should be valid")
    @Schema(example = "john@example.com")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(example = "NewSecureP@ss1", description = "Leave empty to keep current password")
    private String password;

    @Schema(example = "USER")
    private UserRole role;

    @Schema(example = "ACTIVE")
    private UserStatus status;

    @Schema(example = "+1-555-0100")
    private String phone;

    @Schema(example = "Engineering")
    private String department;

    @Schema(example = "false")
    private Boolean canManageUsers;

    @Schema(example = "false")
    private Boolean canViewReports;

    @Schema(example = "false")
    private Boolean canManageSettings;
}


