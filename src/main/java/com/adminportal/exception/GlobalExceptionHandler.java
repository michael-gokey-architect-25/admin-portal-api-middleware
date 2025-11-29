// admin-portal-api/src/main/java/com/adminportal/exception/GlobalExceptionHandler.java

// ============================================================================
// PURPOSE: Global exception handler for all controllers
// - Catches all exceptions thrown by services
// - Converts to HTTP responses with proper status codes
// - Returns consistent error format to client
// - Logs errors for debugging
// KEY CONCEPTS:
// 1. @ControllerAdvice: Applies to all @RestController classes
// 2. @ExceptionHandler: Maps specific exceptions to handler methods
// 3. ErrorResponse DTO: Consistent error format across API
// 4. HttpStatus: Maps exceptions to HTTP status codes
// 5. Exception Hierarchy: Spring catches exceptions in handler method order
// ============================================================================

package com.adminportal.exception;

import com.adminportal.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    
    
    /** ========== VALIDATION ERRORS =====
     * Handle @Valid validation errors from request bodies
     * HTTP Status: 400 Bad Request
     * Caught when:
     * - @NotBlank constraint violated
     * - @Email constraint violated
     * - @Size constraint violated
     * - Any JSR-303 validation fails
     * Example: POST /auth/login with invalid email
     * → MethodArgumentNotValidException
     * → 400 Bad Request with field errors
     *
     * @param ex Exception containing validation errors
     * @param request HTTP request context
     * @return ResponseEntity with 400 status and error details      */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        List<Map<String, Object>> details = new ArrayList<>();

        // Extract all field validation errors
        ex.getBindingResult().getFieldErrors().forEach(error ->
            details.add(Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage(),
                "rejectedValue", error.getRejectedValue() != null ? error.getRejectedValue() : ""
            ))
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("Input validation failed")
            .timestamp(LocalDateTime.now())
            .details(details)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    
    
    /** ===== CUSTOM VALIDATION EXCEPTION ===========
     * Handle custom ValidationException from services
     * HTTP Status: 400 Bad Request
     * Thrown when:
     * - Custom validation logic fails
     * - Business rule violated
     * - Data format invalid
     * Example: Password too short
     * → ValidationException("Password must be 8+ characters")
     * → 400 Bad Request
     *
     * @param ex Exception with validation message
     * @param request HTTP request context
     * @return ResponseEntity with 400 status      */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    
    
    /** ==== AUTHENTICATION ERROR =======
     * Handle AuthenticationException
     * HTTP Status: 401 Unauthorized
     * Thrown when:
     * - Invalid email/password
     * - JWT token invalid or expired
     * - Account not active (suspended/inactive)
     * - Refresh token revoked
     * Example: POST /auth/login with wrong password
     * → AuthenticationException("Invalid email or password")
     * → 401 Unauthorized
     *
     * @param ex Exception with auth failure reason
     * @param request HTTP request context
     * @return ResponseEntity with 401 status      */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("AUTHENTICATION_ERROR")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    
    
    /** === FORBIDDEN ERROR ======
     * Handle ForbiddenException
     * HTTP Status: 403 Forbidden
     * Thrown when:
     * - User lacks required role (e.g., trying to delete user without ADMIN)
     * - User trying to modify another user's profile (without ADMIN)
     * - Insufficient permissions
     * Example: DELETE /users/123 as regular user
     * → ForbiddenException("Admin role required")
     * → 403 Forbidden
     *
     * @param ex Exception with permission error
     * @param request HTTP request context
     * @return ResponseEntity with 403 status      */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            WebRequest request) {
        log.warn("Access forbidden: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("FORBIDDEN")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    
    
    /** ========== DUPLICATE RESOURCE ERROR ========
     * Handle DuplicateResourceException
     * HTTP Status: 409 Conflict
     * Thrown when:
     * - Email already registered
     * - Username already taken
     * - Any unique constraint violation
     * Example:  POST /auth/register with existing email
     * → DuplicateResourceException("Email already registered")
     * → 409 Conflict
     *
     * @param ex Exception with duplicate resource info
     * @param request HTTP request context
     * @return ResponseEntity with 409 status      */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("DUPLICATE_RESOURCE")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    
    
    /** ========== NOT FOUND ERROR ========
     * Handle ResourceNotFoundException
     * HTTP Status: 404 Not Found
     * Thrown when:
     * - User ID doesn't exist
     * - Token not found in database
     * - Any resource lookup fails
     * Example:  GET /users/invalid-id
     * → ResourceNotFoundException("User not found")
     * → 404 Not Found
     *
     * @param ex Exception with resource not found message
     * @param request HTTP request context
     * @return ResponseEntity with 404 status     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    
    
    /** === GENERIC EXCEPTION HANDLER ================
     * Handle all unexpected exceptions,
     * HTTP Status: 500 Internal Server Error, Catches:
     * - NullPointerException
     * - SQLException
     * - Any other RuntimeException not specifically handled
     * This is the safety net - should rarely be hit in production
     * if custom exceptions are thrown properly
     *
     * @param ex Any unexpected exception
     * @param request HTTP request context
     * @return ResponseEntity with 500 status      */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred. Please try again later.")
            .timestamp(LocalDateTime.now())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}


