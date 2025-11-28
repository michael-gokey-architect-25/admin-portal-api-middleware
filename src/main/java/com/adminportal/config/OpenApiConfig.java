// admin-portal-api/src/main/java/com/adminportal/config/OpenApiConfig.java

// ============================================================================
// PURPOSE: Configuration for Swagger/OpenAPI documentation
// - Customizes Swagger UI appearance
// - Configures API documentation endpoints
// - Available at: http://localhost:8080/api/swagger-ui.html
// ============================================================================

package com.adminportal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * Customizes Swagger UI and API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:AdminPortal API}")
    private String appName;

    @Value("${spring.application.version:1.0.0}")
    private String appVersion;

    /**
     * Configure OpenAPI documentation
     * Accessible at: http://localhost:8080/api/swagger-ui.html
     * JSON spec: http://localhost:8080/api/v3/api-docs
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(appName)
                .version(appVersion)
                .description("Enterprise User Management System with JWT Authentication")
                .contact(new Contact()
                    .name("AdminPortal Team")
                    .email("admin@adminportal.com")
                )
                .license(new License()
                    .name("MIT")
                )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Development Server"),
                new Server()
                    .url("https://api.adminportal.com")
                    .description("Production Server")
            ));
    }
}

public class OpenApiConfig {
    
}
