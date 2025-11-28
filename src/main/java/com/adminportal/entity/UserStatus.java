
// admin-portal-api/src/main/java/com/adminportal/entity/UserStatus.java
// ============================================================================
// PURPOSE: Enum for user account status
// - Controls whether user can login
// - Used for soft deletes and account management
// ============================================================================

package com.adminportal.entity;

/**
 * User Status - Enumeration of valid user account statuses
 *
 * - ACTIVE: User account is active and can login
 * - INACTIVE: User account exists but is disabled (cannot login)
 * - SUSPENDED: User account suspended due to violations
 */
public enum UserStatus {
    ACTIVE,     // User can login
    INACTIVE,   // User account disabled
    SUSPENDED   // User account suspended (temporary block)
}


// public class UserStatus {
    
// }


