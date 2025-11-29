// admin-portal-api/src/main/java/com/adminportal/exception/ValidationException.java

// ============================================================================
// PURPOSE: Exception thrown when input validation fails
// - Maps to HTTP 400 Bad Request
// - Used for invalid email format, password length, etc.
// ============================================================================

package com.adminportal.exception;

public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
