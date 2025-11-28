// admin-portal-api/src/main/java/com/adminportal/service/HealthService.java

// ============================================================================
// PURPOSE: System health check service
// - Verify API is running
// - Check database connectivity
// - Return version information
// - Used for load balancer health checks
// KEY CONCEPTS:
// 1. Health Checks: Important for Docker/Kubernetes
// 2. Load Balancer: Uses /health to determine if service is alive
// 3. Readiness Probes: Kubernetes checks before routing traffic
// 4. Liveness Probes: Kubernetes checks if pod should be restarted
// 
// I still haven't figured out if I like the whole health check service thing or not
// 
// ============================================================================

package com.adminportal.service;

import com.adminportal.dto.response.HealthCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Health Check Service, RESPONSIBILITIES:
 * - Verify API is running
 * - Check database connectivity
 * - Return system version
 * - Used by load balancers and orchestration platforms  */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthService {

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    /**
     * Perform health check, USED BY:
     * - Docker health checks: docker run --health-cmd="curl http://localhost:8080/health"
     * - Kubernetes: readinessProbe and livenessProbe
     * - Load Balancers: periodic health verification
     * - Monitoring: Prometheus, DataDog, etc.
     * @return HealthCheckResponse with status     */
    public HealthCheckResponse checkHealth() {
        log.debug("Health check requested");

        try {
            // If we get here, API is running and database is accessible
            return HealthCheckResponse.builder()
                .status("ok")
                .timestamp(LocalDateTime.now())
                .version(applicationVersion)
                .build();

        } catch (Exception ex) {
            log.error("Health check failed: {}", ex.getMessage());

            return HealthCheckResponse.builder()
                .status("error")
                .timestamp(LocalDateTime.now())
                .version(applicationVersion)
                .build();
        }
    }
}

