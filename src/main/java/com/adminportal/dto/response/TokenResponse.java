// admin-portal-api/src/main/java/com/adminportal/dto/response/TokenResponse.java

// ============================================================================
// PURPOSE: Response DTO for token refresh - Returns new access token only (no user data)
// ============================================================================

package com.adminportal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Token refresh response")
public class TokenResponse {

    @Schema(description = "New JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Token expiration time in milliseconds", example = "3600000")
    private Long expiresIn;
}

