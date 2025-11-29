// admin-portal-api/src/main/java/com/adminportal/exception/DuplicateResourceException.java

// ============================================================================
// PURPOSE: Exception thrown when a duplicate resource is detected
// - Maps to HTTP 409 Conflict
// - Used when email or username already exists
// ============================================================================

package com.adminportal.exception;

public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
