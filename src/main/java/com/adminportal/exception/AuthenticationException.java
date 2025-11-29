// admin-portal-api/src/main/java/com/adminportal/exception/AuthenticationException.java

// ============================================================================
// PURPOSE: Exception thrown when authentication fails
// - Maps to HTTP 401 Unauthorized
// - Used for invalid credentials, expired tokens, etc.
// ============================================================================

package com.adminportal.exception;

public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
