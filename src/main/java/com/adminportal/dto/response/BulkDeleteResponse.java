// admin-portal-api/src/main/java/com/adminportal/dto/response/BulkDeleteResponse.java

// ============================================================================
// Response DTO - bulk delete operation results, reports number of deleted & failed deletions
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
@Schema(description = "Bulk delete operation results")
public class BulkDeleteResponse {

    @Schema(description = "Number of users successfully deleted", example = "2")
    private Integer deletedCount;

    @Schema(description = "Number of users failed to delete", example = "0")
    private Integer failedCount;

    @Schema(description = "Operation summary message", example = "2 users deleted successfully")
    private String message;
}

