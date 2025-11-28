// admin-portal-api/src/main/java/com/adminportal/dto/request/RegisterRequest.java

// ============================================================================
// PURPOSE: Request DTO for user registration endpoint
// - More fields than LoginRequest (name fields)
// - Password confirmation not included (handled client-side)
// ============================================================================

package com.adminportal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "User registration data")
public record RegisterRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    @Schema(description = "First name", example = "John")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    @Schema(description = "Last name", example = "Doe")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Email address", example = "john@example.com")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password (minimum 8 characters)", example = "SecureP@ss1")
    String password
) {}

