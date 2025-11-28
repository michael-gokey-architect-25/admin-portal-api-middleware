// admin-portal-api/src/main/java/com/adminportal/dto/response/LoginResponse.java
// ============================================================================
// PURPOSE: Response DTO for successful login
// - Returns JWT tokens and user information
// - Used by client to store credentials
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
@Schema(description = "Successful login response")
public class LoginResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh token for getting new access token", example = "refresh_token_xyz")
    private String refreshToken;

    @Schema(description = "Token expiration time in milliseconds", example = "3600000")
    private Long expiresIn;

    @Schema(description = "User information")
    private UserResponse user;
}
public class LoginResponse {
    
}
