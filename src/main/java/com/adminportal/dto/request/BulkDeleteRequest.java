// admin-portal-api/src/main/java/com/adminportal/dto/request/BulkDeleteRequest.java

// ============================================================================
// PURPOSE: Request DTO for bulk delete operation - Array of user IDs to delete
// ============================================================================

package com.adminportal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bulk delete users")
public record BulkDeleteRequest(
    @NotEmpty(message = "At least one user ID is required")
    @Schema(description = "List of user IDs to delete")
    List<UUID> userIds
) {}

