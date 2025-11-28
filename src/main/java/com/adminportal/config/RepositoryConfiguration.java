// admin-portal-api/src/main/java/com/adminportal/config/RepositoryConfiguration.java

// ============================================================================
// PURPOSE: Configuration for Spring Data JPA repositories
// - Enables @Query annotations
// - Configures audit fields (CreatedBy, LastModifiedBy)
// - Base package scanning
// ============================================================================

package com.adminportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Data JPA Repository Configuration
 *
 * FEATURES ENABLED:
 * 1. @EnableJpaRepositories: Auto-creates repository beans
 * 2. @EnableJpaAuditing: Enables @CreationTimestamp, @UpdateTimestamp
 * 3. Query DSL support: Custom @Query methods
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.adminportal.repository")
@EnableJpaAuditing
public class RepositoryConfiguration {
}


public class RepositoryConfiguration {
    
}
