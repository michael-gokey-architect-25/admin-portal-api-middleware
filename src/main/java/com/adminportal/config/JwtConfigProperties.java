
// admin-portal-api/src/main/java/com/adminportal/config/JwtConfigProperties.java
// ============================================================================
// PURPOSE: Configuration properties for JWT settings
// - Binds application.yml jwt.* properties to Java class
// - Allows type-safe access to configuration values
// - Auto-validated by Spring
// KEY CONCEPTS:
// 1. @ConfigurationProperties: Binds YAML to Java class
// 2. Properties can be overridden by environment variables
// 3. Validation ensures configuration is correct
// ============================================================================

package com.adminportal.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT Configuration Properties
 *
 * Binds to application.yml:
 * jwt:
 *   secret: "..."
 *   expiration: 3600000
 *   refresh-expiration: 604800000
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtConfigProperties {

    /**
     * Secret key for signing JWT tokens
     * Must be at least 32 characters for HS256
     * Load from environment variable in production: JWT_SECRET
     */
    private String secret = "your-super-secret-key-change-this-in-production-at-least-32-characters-long";

    /**
     * Access token expiration time in milliseconds
     * Default: 3600000 ms (60 minutes)
     * Can be overridden: JWT_EXPIRATION env var or jwt.expiration property
     */
    private Long expiration = 3600000L;

    /**
     * Refresh token expiration time in milliseconds
     * Default: 604800000 ms (7 days)
     * Can be overridden: JWT_REFRESH_EXPIRATION env var or jwt.refresh-expiration property
     */
    private Long refreshExpiration = 604800000L;
}

public class JwtConfigProperties {
    
}
