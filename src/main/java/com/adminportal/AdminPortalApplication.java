// admin-portal-api/src/main/java/com/adminportal/AdminPortalApplication.java
// ============================================================================
// PURPOSE: Main application entry point
// - Spring Boot application launcher
// - Configures auto-scanning of components
// - Initializes application context
// KEY CONCEPTS:
// 1. @SpringBootApplication: Enables auto-configuration and component scanning
// 2. main() method: JVM entry point for Java application
// 3. SpringApplication.run(): Starts Spring Boot embedded Tomcat
// TEACHING NOTES:
// - @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
// - Auto-scanning finds @Component, @Service, @Repository, @Controller classes
// - Application context loads all beans defined in classpath
// ============================================================================

package com.adminportal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AdminPortal Application - Main entry point, STARTUP PROCESS:
 * 1. SpringApplication.run() is called with this class and application args
 * 2. Spring Boot auto-configures based on classpath dependencies
 * 3. Component scanning finds all @Component, @Service, @Repository classes
 * 4. Application context is created and beans are instantiated
 * 5. Embedded Tomcat starts on configured port (default 8080)
 * 6. Application is ready to accept requests
 * CONFIGURATION:
 * - application.yml: Server port, database, JWT settings
 * - Profiles: dev, prod, local can be activated
 * - Run: mvn spring-boot:run
 * - Run with profile: mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev" */
@SpringBootApplication
@EnableTransactionManagement  // Enable @Transactional annotation support
@EnableAsync                   // Enable @Async annotation for async methods
@OpenAPIDefinition(
    info = @Info(
        title = "AdminPortal Backend API",
        version = "1.0.0",
        description = "RESTful API for AdminPortal v1 - User Management System with JWT Authentication",
        contact = @Contact(
            name = "AdminPortal Team",
            email = "admin@adminportal.com"
        ),
        license = @License(
            name = "MIT"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080/api",
            description = "Development Server"
        ),
        @Server(
            url = "https://api.adminportal.com",
            description = "Production Server"
        )
    }
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication"
)
public class AdminPortalApplication {

    /**
     * Main method - JVM entry point
     * Starts Spring Boot application
     *
     * @param args Command line arguments
     *             Example: --spring.profiles.active=prod
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminPortalApplication.class, args);
    }

    /**
     * Application startup message
     * Printed after application successfully starts
     */
    @Bean
    public ApplicationStartupListener applicationStartupListener() {
        return new ApplicationStartupListener();
    }

    /**
     * Inner class to log application startup
     */
    public static class ApplicationStartupListener {
        public ApplicationStartupListener() {
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║                                                                ║");
            System.out.println("║           AdminPortal API v1.0.0 Successfully Started          ║");
            System.out.println("║                                                                ║");
            System.out.println("║  📍 API Base URL:  http://localhost:8080/api                   ║");
            System.out.println("║  📋 Swagger UI:    http://localhost:8080/api/swagger-ui.html   ║");
            System.out.println("║  🏥 Health Check:  http://localhost:8080/api/health            ║");
            System.out.println("║                                                                ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
        }
    }
}

public class AdminPortalApplication {
    
}
