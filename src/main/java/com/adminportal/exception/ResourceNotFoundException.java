// admin-portal-api/src/main/java/com/adminportal/exception/ResourceNotFoundException.java

// ============================================================================
// PURPOSE: Exception thrown when a requested resource is not found
// - Maps to HTTP 404 Not Found
// - Used when user, token, or other entity doesn't exist
// ============================================================================

package com.adminportal.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
