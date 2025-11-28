
//  admin-portal-api/src/main/java/com/adminportal/entity/UserRole.java
// ============================================================================
// PURPOSE: Enum for user roles (type-safe alternative to strings)
// - Prevents invalid role values
// - Used in database as VARCHAR and in Java as enum
// - Provides clear list of valid roles in codebase
// ============================================================================

package com.adminportal.entity;

/**
 * User Roles - Enumeration of valid user roles
 *
 * Each role grants different permissions:
 * - ADMIN: Full system access, user management, settings management
 * - MANAGER: Team management, report viewing, limited user actions
 * - USER: Basic user access, personal profile management only
 */
public enum UserRole {
    ADMIN,      // System administrator with full access
    MANAGER,    // Manager with team oversight
    USER        // Regular user with limited access
}

public class UserRole {
    
}
