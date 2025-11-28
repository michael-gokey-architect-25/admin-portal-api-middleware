// admin-portal-api/src/main/java/com/adminportal/dto/response/ErrorResponse.java

// ============================================================================
// PURPOSE: Standard error response for all API errors
// - Used by GlobalExceptionHandler
// - Provides consistent error format across API
// ============================================================================

package com.adminportal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    private String code;

    @Schema(description = "Error message", example = "Email is required")
    private String message;

    @Schema(description = "Timestamp of error")
    private LocalDateTime timestamp;

    @Schema(description = "Additional error details (validation errors)")
    private List<Map<String, Object>> details;
}

