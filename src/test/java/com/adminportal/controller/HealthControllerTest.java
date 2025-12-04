// src/test/java/com/adminportal/controller/HealthControllerTest.java

// ============================================================================
// Testing Strategy:
// - Use @WebMvcTest to load only controller layer
// - Mock HealthService dependency
// - Test public health check endpoint (no authentication required)
// - Verify response structure and status codes
// - Test both UP and DOWN health states
// ============================================================================

package com.adminportal.controller;

import com.adminportal.service.HealthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("HealthController Unit Tests")
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HealthService healthService;

    // ========================================================================
    // TEST FIXTURES
    // ========================================================================
    private HealthCheckResponse healthResponse;

    @BeforeEach
    void setUp() {
        // Create health check response
        healthResponse = HealthCheckResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .database("UP")
                .jwt("UP")
                .securityContext("UP")
                .version("1.0.0")
                .build();
    }


    // ========================================================================
    // HEALTH CHECK TESTS - GET /health
    // ========================================================================
    @Test
    @DisplayName("HEALTH - Should return 200 OK when system is healthy")
    void testHealth_Success_SystemUp() throws Exception {
        // Arrange
        when(healthService.checkHealth())
                .thenReturn(healthResponse);

        // Act & Assert
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.jwt").value("UP"))
                .andExpect(jsonPath("$.securityContext").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(healthService).checkHealth();
    }


    @Test
    @DisplayName("HEALTH - Should be publicly accessible (no authentication required)")
    void testHealth_PublicAccess() throws Exception {
        // Arrange
        when(healthService.checkHealth())
                .thenReturn(healthResponse);

        // Act & Assert - No @WithMockUser annotation, should still work (public endpoint)
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        verify(healthService).checkHealth();
    }


    @Test
    @DisplayName("HEALTH - Should return error state when database is down")
    void testHealth_DatabaseDown() throws Exception {
        // Arrange
        HealthCheckResponse unhealthyResponse = HealthCheckResponse.builder()
                .status("DOWN")
                .timestamp(LocalDateTime.now())
                .database("DOWN")
                .jwt("UP")
                .securityContext("UP")
                .version("1.0.0")
                .error("Database connection failed")
                .build();

        when(healthService.checkHealth())
                .thenReturn(unhealthyResponse);

        // Act & Assert
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isServiceUnavailable()) // 503 for DOWN status
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("DOWN"))
                .andExpect(jsonPath("$.error").value("Database connection failed"));

        verify(healthService).checkHealth();
    }


    @Test
    @DisplayName("HEALTH - Should return JSON content type")
    void testHealth_ResponseContentType() throws Exception {
        // Arrange
        when(healthService.checkHealth())
                .thenReturn(healthResponse);

        // Act & Assert
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(healthService).checkHealth();
    }


    @Test
    @DisplayName("HEALTH - Response should contain all required health components")
    void testHealth_ResponseStructure() throws Exception {
        // Arrange
        when(healthService.checkHealth())
                .thenReturn(healthResponse);

        // Act
        MvcResult result = mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.database").exists())
                .andExpect(jsonPath("$.jwt").exists())
                .andExpect(jsonPath("$.securityContext").exists())
                .andExpect(jsonPath("$.version").exists())
                .andReturn();

        // Assert - Verify response is valid JSON
        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("status"));
        assertTrue(responseBody.contains("UP"));
    }


    @Test
    @DisplayName("HEALTH - Should have proper response headers")
    void testHealth_ResponseHeaders() throws Exception {
        // Arrange
        when(healthService.checkHealth())
                .thenReturn(healthResponse);

        // Act & Assert
        mockMvc.perform(get("/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("Content-Type", 
                        org.hamcrest.Matchers.containsString("application/json")));

        verify(healthService).checkHealth();
    }



    // ========================================================================
    // HEALTH CHECK DTO Adjust based on your actual HealthCheckResponse implementation
    // ========================================================================
    static class HealthCheckResponse {
        private String status;
        private LocalDateTime timestamp;
        private String database;
        private String jwt;
        private String securityContext;
        private String version;
        private String error;

        private HealthCheckResponse(Builder builder) {
            this.status = builder.status;
            this.timestamp = builder.timestamp;
            this.database = builder.database;
            this.jwt = builder.jwt;
            this.securityContext = builder.securityContext;
            this.version = builder.version;
            this.error = builder.error;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getDatabase() { return database; }
        public String getJwt() { return jwt; }
        public String getSecurityContext() { return securityContext; }
        public String getVersion() { return version; }
        public String getError() { return error; }

        static class Builder {
            private String status;
            private LocalDateTime timestamp;
            private String database;
            private String jwt;
            private String securityContext;
            private String version;
            private String error;

            Builder status(String status) {
                this.status = status;
                return this;
            }

            Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            Builder database(String database) {
                this.database = database;
                return this;
            }

            Builder jwt(String jwt) {
                this.jwt = jwt;
                return this;
            }

            Builder securityContext(String securityContext) {
                this.securityContext = securityContext;
                return this;
            }

            Builder version(String version) {
                this.version = version;
                return this;
            }

            Builder error(String error) {
                this.error = error;
                return this;
            }

            HealthCheckResponse build() {
                return new HealthCheckResponse(this);
            }
        }
    }

    /* ========================================================================
    // TEST EXECUTION TIPS:
    Run individual test:
        mvn test -Dtest=HealthControllerTest#testHealth_Success_SystemUp
    Run all HealthController tests:
        mvn test -Dtest=HealthControllerTest
    Run with detailed output:
        mvn test -Dtest=HealthControllerTest -X
    Run with coverage:
        mvn clean test jacoco:report
    Expected output:
    [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0     */
}

