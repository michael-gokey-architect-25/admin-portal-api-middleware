// admin-portal-api/src/main/java/com/adminportal/dto/request/RefreshTokenRequest.java

// ============================================================================
// PURPOSE: Request DTO for token refresh endpoint - Single field: refresh token string
// ============================================================================

package com.adminportal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token request")
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token", example = "refresh_token_xyz...")
    String refreshToken
) {}


