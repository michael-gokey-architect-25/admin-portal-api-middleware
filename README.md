# Java Spring Boot API Middleware Design

**NOTES: I AM NOT A JAVA programmer**,  *I am a pretty good pattern guy. copy pasta. Been that way since I first started look at WebSphere Host Publisher code in 2000, and then later learned it was a form of EJB v1. Then I learned Servlet v1 & v2, at the same time with those old red Wrox books. Then EJB v2.... and then bounced. Then later did POJO work, layers and patterns. Still never learned Java. Just copy pasta. I can see examples, amples, and make it work. 
And then at Cognizant Hartford, I spent hours "learning" / mastering the Spring Boot API designs.*

*Remarkdly, I can do the same with .NET API work. Simular patterns, once you undertand the layers and the OpenAPI Swagger doc.*

*See the Wiki for Notes:* 
* [admin-portal-api-middleware/wiki](https://github.com/michael-gokey-architect-25/admin-portal-api-middleware/wiki)  UPDATED! 


#### Next steps
I know it looks like a lot of broken stuff. But it takes time to figure out each step
- 4 [**Service Layer**](https://github.com/michael-gokey-architect-25/admin-portal-api-middleware/wiki/Service-Layer-Architecture) (AuthService, UserService),  (DONE)
- 5 [**Controller Layer**](https://github.com/michael-gokey-architect-25/admin-portal-api-middleware/wiki/Controller-Layer-Architecture), (DONE)
- 6 [**Repository Layer**](https://github.com/michael-gokey-architect-25/admin-portal-api-middleware/wiki/Repository-Layer), (DONE)
- 7 [Exception Handling](https://github.com/michael-gokey-architect-25/admin-portal-api-middleware/wiki/Exception-Handling), (DONE)
- 8 Unit & Integration Tests (this push commit, still in progress),
- 9 Database Migration work (not yet started)

==========================================

## AdminPortal v1 Backend Architecture
### Project Overview

AdminPortal v1 is an enterprise-grade web application built with **Angular 16** using a **traditional module-based architecture**. The application focuses on authentication, role-based access control (RBAC), and user management, with **NgRx** as the central state management solution.
This repo part is the Java Spring Boot API Middleware, built with **Spring Boot**, **Spring Security**,  **Spring Data JPA**,  **Spring Validation**,  **Spring OpenAPI 3**

There is a Data repo, data-portal, and a Multi-Cloud Highly Available Web Application design. 







## 1. AdminPortal v1 Backend Architecture

Drawing a blank at the moment. 


## 2. API Services Architecture

### **Service Layer Organization**

Based on the OpenAPI spec, organize into **3 core services**:

#### **1. AuthService** (Authentication & Token Management)
- Handles JWT generation, validation, refresh
- Token lifecycle management
- User credential validation

#### **2. UserService** (User Management)
- CRUD operations
- Search functionality
- Bulk operations
- User profile management

#### **3. HealthService** (System Health)
- Health checks
- Operational monitoring

---



## 3. API Methods Summary

### **Total: 11 Endpoints Across 3 Controllers**

#### **AuthController (4 endpoints)**
```
POST   /auth/login          - User login
POST   /auth/register       - User registration  
POST   /auth/refresh        - Refresh JWT token
POST   /auth/logout         - User logout
```

#### **UserController (7 endpoints)**
```
GET    /users               - List all users (paginated, filtered, sorted)
POST   /users               - Create user
GET    /users/search        - Search users
POST   /users/bulk-delete   - Bulk delete users
GET    /users/{id}          - Get user by ID
PUT    /users/{id}          - Update user
DELETE /users/{id}          - Delete user
GET    /users/{id}/profile  - Get user profile
```

#### **HealthController (1 endpoint)**
```
GET    /health              - Health check
```

---


## 4. Spring Boot Project Structure

### **Directory Layout**

```
admin-portal-api/
├── pom.xml                              # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/com/adminportal/
│   │   │   ├── AdminPortalApplication.java
│   │   │   │
│   │   │   ├── config/                  # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   │
│   │   │   ├── controller/              # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── HealthController.java
│   │   │   │
│   │   │   ├── service/                 # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── HealthService.java
│   │   │   │
│   │   │   ├── repository/              # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   │
│   │   │   ├── entity/                  # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   └── RefreshToken.java
│   │   │   │
│   │   │   ├── dto/                     # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   ├── CreateUserRequest.java
│   │   │   │   │   └── UpdateUserRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── LoginResponse.java
│   │   │   │       ├── TokenResponse.java
│   │   │   │       ├── UserListResponse.java
│   │   │   │       ├── BulkDeleteResponse.java
│   │   │   │       └── ErrorResponse.java
│   │   │   │
│   │   │   ├── security/                # Security & JWT
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── AuthenticationController.java
│   │   │   │
│   │   │   ├── exception/               # Custom exceptions
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── DuplicateResourceException.java
│   │   │   │   ├── AuthenticationException.java
│   │   │   │   ├── AuthorizationException.java
│   │   │   │   ├── ValidationException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   ├── util/                    # Utility classes
│   │   │   │   ├── ValidationUtil.java
│   │   │   │   └── PaginationUtil.java
│   │   │   │
│   │   │   ├── mapper/                  # Entity/DTO mappers
│   │   │   │   └── UserMapper.java
│   │   │   │
│   │   │   └── validator/               # Custom validators
│   │   │       └── EmailValidator.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   │
│   └── test/
│       └── java/com/adminportal/
│           ├── controller/
│           ├── service/
│           ├── repository/
│           └── security/
│
├── .gitignore
├── README.md
└── Dockerfile
```

---



## 5. Core Technologies & Dependencies

### **Spring Boot 3.1.x Stack**
- **Spring Boot**: 3.1.x (Latest stable, Java 17+ compatible)
- **Spring Security**: 6.1.x (JWT, RBAC, OAuth2 ready)
- **Spring Data JPA**: ORM abstraction
- **Spring Validation**: Bean Validation (JSR-303/JSR-380)
- **Spring OpenAPI 3**: Swagger/OpenAPI documentation

### **Database**
- **PostgreSQL**: Production-grade RDBMS
- **H2**: In-memory for testing

### **Security & JWT**
- **jjwt (JSON Web Token)**: 0.12.x for JWT handling
- **bcrypt**: Password hashing via Spring Security

### **Utilities**
- **Lombok**: Reduce boilerplate (annotations for getters/setters)
- **MapStruct**: Entity/DTO mapping
- **Validation**: jakarta.validation:jakarta.validation-api

### **Testing**
- **JUnit 5**: Test framework
- **Mockito**: Mocking
- **Spring Boot Test**: Integration testing
- **Testcontainers**: PostgreSQL in Docker for tests

---



## 6. Key Design Patterns

### **Layered Architecture**
```
Controller Layer
    ↓ (Request/Response)
Service Layer
    ↓ (Business Logic)
Repository Layer
    ↓ (Data Access)
Database
```


### **Security Flow**
```
Request with JWT
    ↓
JwtAuthenticationFilter
    ↓
JwtTokenProvider (Validates)
    ↓
CustomUserDetailsService (Loads User)
    ↓
SecurityContext (Sets Principal)
    ↓
Controller Method (with @PreAuthorize)
```

### **Exception Handling**
- Global `@ControllerAdvice` for centralized exception handling
- Consistent `ErrorResponse` format for all errors
- Proper HTTP status codes (400, 401, 403, 404, 409)

---



## 7. API Response Consistency

### **Success Response**
```json
{
  "data": { /* entity or list */ },
  "pagination": { /* optional */ },
  "timestamp": "2024-01-15T10:30:00Z",
  "status": "success"
}
```

### **Error Response**
```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "timestamp": "2024-01-15T10:30:00Z",
  "details": [ /* validation errors */ ]
}
```

---



## 8. Security Implementation

### **Authentication**
- JWT with RS256 (RSA) or HS256 (HMAC) signing
- Access token: 15-60 minutes
- Refresh token: 7 days stored in DB

### **Authorization**
- Role-based access control (RBAC): ADMIN, MANAGER, USER
- Permission-based checks via `@PreAuthorize` on methods
- Method-level security annotations

### **Endpoint Protection**
```
/auth/login      → Public (no auth)
/auth/register   → Public (no auth)
/auth/refresh    → Public (no auth required, but JWT validated)
/auth/logout     → Protected (requires JWT)
/users/**        → Protected (requires JWT + ADMIN role)
/health          → Public
```

---



## 9. Implementation Roadmap

### **Phase 1: Project Setup (1-2 days)**
- Initialize Maven project with Spring Boot
- Configure pom.xml with all dependencies
- Set up application.yml (dev, prod profiles)
- Configure database (PostgreSQL)

### **Phase 2: Core Infrastructure (2-3 days)**
- JPA entities (User, RefreshToken)
- Database migrations (Flyway or Liquibase)
- Custom exceptions and global exception handler
- Logging configuration

### **Phase 3: Security Layer (2-3 days)**
- JWT token provider
- Authentication filter
- Custom UserDetailsService
- Security configuration

### **Phase 4: Auth Service (2-3 days)**
- AuthController + AuthService
- Login, register, refresh, logout logic
- Token management and validation

### **Phase 5: User Service (3-4 days)**
- UserController + UserService
- CRUD operations
- Search, filter, pagination
- Bulk delete functionality

### **Phase 6: Configuration & Documentation (1-2 days)**
- OpenAPI/Swagger integration
- API documentation
- CORS configuration

### **Phase 7: Testing & Deployment (2-3 days)**
- Unit tests (services, controllers, security)
- Integration tests
- Docker containerization
- Deployment scripts

**Total Timeline: 2-3 weeks for production-ready API**

---



## 10. Deployment Architecture

### **Docker Containerization**
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/admin-portal-api-1.0.0.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
```


### **Docker Compose (Local Development)**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: admin_portal
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
  
  api:
    build: .
    ports:
      - "3000:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/admin_portal
    depends_on:
      - postgres
```


#### **Kubernetes (Production)**
- Deployment for API service
- Service for load balancing
- ConfigMap for environment variables
- Secrets for sensitive data (JWT keys, DB credentials!) 

---



## 11. CI/CD Pipeline

### **GitHub Actions Workflow**
```
On push to main:
  1. Build Maven project
  2. Run unit tests
  3. Run integration tests
  4. Build Docker image
  5. Push to Docker registry
  6. Deploy to Kubernetes
  7. Run smoke tests
  8. Post deployment checks
```

---



## Summary

| Aspect | Decision |
|--------|----------|
| **Build Tool** | Maven 3.8.x+ |
| **API Services** | 3 (Auth, User, Health) |
| **API Endpoints** | 11 methods |
| **Controllers** | 3 (AuthController, UserController, HealthController) |
| **Java Version** | 17 LTS |
| **Spring Boot** | 3.1.x |
| **Database** | PostgreSQL 15 |
| **Security** | JWT + Spring Security |
| **Containerization** | Docker + Compose + Kubernetes |
| **Development Timeline** | 2-3 weeks |


Lets find out how well I can cobble together a full-stack with a multitude of languages in mixed multi-cloud environment.

/*
src/test/
├── java/com/adminportal/
│   ├── service/
│   │   ├── AuthServiceTest.java
│   │   ├── UserServiceTest.java
│   │   └── HealthServiceTest.java
│   ├── controller/
│   │   ├── AuthControllerTest.java
│   │   └── UserControllerTest.java
│   ├── repository/
│   │   ├── UserRepositoryTest.java
│   │   └── RefreshTokenRepositoryTest.java
│   ├── security/
│   │   ├── JwtTokenProviderTest.java
│   │   └── CustomUserDetailsServiceTest.java
│   ├── integration/
│   │   ├── AuthIntegrationTest.java
│   │   └── UserIntegrationTest.java
│   └── fixtures/
│       └── TestDataBuilder.java (helper for creating test data)
└── resources/
    ├── application-test.yml
    └── logback-test.xml
*/
