// admin-portal-api/src/main/java/com/adminportal/dto/response/UserListResponse.java


// ============================================================================
// PURPOSE: Response wrapper with pagination metadata
// - Includes user list
// - Includes pagination info (current page, total, etc.)
// - Sent to client for paginated results
// ============================================================================

package com.adminportal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User List Response with Pagination, STRUCTURE:
 * {
 *   "data": [ { user1 }, { user2 }, ... ],
 *   "pagination": {
 *     "page": 1,
 *     "limit": 10,
 *     "total": 50,
 *     "totalPages": 5
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated user list response")
public class UserListResponse {

    @Schema(description = "Array of users")
    private List<UserResponse> data;

    @Schema(description = "Pagination metadata")
    private Pagination pagination;

    /**
     * Pagination Metadata, FIELDS:
     * - page: Current page (1-indexed)
     * - limit: Items per page
     * - total: Total items across all pages
     * - totalPages: Total number of pages    */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Pagination information")
    public static class Pagination {

        @Schema(description = "Current page number (1-indexed)", example = "1")
        private Integer page;

        @Schema(description = "Items per page", example = "10")
        private Integer limit;

        @Schema(description = "Total items across all pages", example = "50")
        private Integer total;

        @Schema(description = "Total number of pages", example = "5")
        private Integer totalPages;
    }
}

