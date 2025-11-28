// admin-portal-api/src/main/java/com/adminportal/controller/HealthController.java

// ============================================================================
// PURPOSE: REST Controller for health check endpoint
// - No authentication required
// - Used by load balancers and orchestration
// - Returns system status
// ============================================================================

package com.adminportal.controller;

import com.adminportal.dto.response.HealthCheckResponse;
import com.adminportal.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check Controller, ENDPOINT:  GET /health
 * PUBLIC ENDPOINT - NO AUTHENTICATION REQUIRED
 * USED BY:
 * - Docker health checks
 * - Kubernetes readinessProbe / livenessProbe
 * - Load balancers
 * - Monitoring systems (Prometheus, DataDog, etc.)  */
@RestController
@Tag(name = "Health", description = "API health check")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    // ==================== DEPENDENCIES ====================
    private final HealthService healthService;



    
    /** ======== HEALTH CHECK ============
     * GET /health
     * RESPONSE (200 OK):
     * {
     *   "status": "ok",
     *   "timestamp": "2024-01-15T10:30:00Z",
     *   "version": "1.0.0"
     * }
     * ERROR (500 Internal Server Error):
     * {
     *   "status": "error",
     *   "timestamp": "2024-01-15T10:30:00Z",
     *   "version": "1.0.0"
     * }
     * AUTHORIZATION:
     * - NO authentication required
     * - Public endpoint
     * FLOW:
     * 1. Receive GET /health
     * 2. Call HealthService.checkHealth()
     * 3. Service tries simple database query
     * 4. If success: return "ok"
     * 5. If error: return "error"
     * 6. Always return 200 (even if "error" status)
     *    Load balancers interpret response body
     *
     * @return ResponseEntity with HealthCheckResponse    */
    @GetMapping("/health")
    @Operation(
        summary = "Health Check",
        description = "Check if API is running"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API is running",
        content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))
    )
    public ResponseEntity<HealthCheckResponse> checkHealth() {
        log.debug("Health check requested");

        // ================ CALL SERVICE ==================
        HealthCheckResponse response = healthService.checkHealth();

        // ============ RETURN 200 OK =============
        // Always return 200, load balancer checks response body
        return ResponseEntity.ok(response);
    }
}


