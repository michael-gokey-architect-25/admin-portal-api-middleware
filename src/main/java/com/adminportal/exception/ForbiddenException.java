// admin-portal-api/src/main/java/com/adminportal/exception/ForbiddenException.java

// ============================================================================
// PURPOSE: Exception thrown when user lacks permissions
// - Maps to HTTP 403 Forbidden
// - Used when user tries to access/modify unauthorized resource
// ============================================================================

package com.adminportal.exception;

public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
