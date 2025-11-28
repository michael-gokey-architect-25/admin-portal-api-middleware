// admin-portal-api/src/main/java/com/adminportal/dto/request/LoginRequest.java
// ============================================================================
// PURPOSE: Request DTO for user login endpoint
// - Receives email and password from client
// - Validated by Spring Validation framework
// - Converted from JSON by Jackson JSON processor
// KEY CONCEPTS:
// 1. DTOs: Separate objects for API contracts (not entities)
// 2. @NotBlank: JSR-303 validation - ensures field is not null/empty
// 3. @Email: Validates email format
// 4. Record: Immutable data transfer object (Java 14+)
// TEACHING NOTES:
// - DTOs shield entities from API changes
// - Validation annotations prevent invalid data from reaching business logic
// - Records are perfect for simple DTOs - immutable by default
// ============================================================================

package com.adminportal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest DTO
 * Represents the payload for POST /auth/login
 */
@Schema(description = "User login credentials")
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User email", example = "user@example.com")
    String email,

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "SecureP@ss1")
    String password
) {}

