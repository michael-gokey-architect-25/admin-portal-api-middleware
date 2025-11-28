// admin-portal-api/src/main/java/com/adminportal/entity/User.java

// ============================================================================
// PURPOSE: JPA Entity representing a user in the system
// - Maps to "users" table in database
// - Contains all user attributes and relationships
// - Includes validation constraints at entity level
// KEY CONCEPTS:
// 1. @Entity: Marks class as JPA entity (maps to DB table)
// 2. @Id & @GeneratedValue: Primary key with auto-generation strategy
// 3. @Column: Specifies column properties (nullable, unique, length)
// 4. Enums: Type-safe representation of roles and statuses
// 5. Temporal fields: Audit timestamps (created, updated, lastLogin)
// TEACHING NOTES:
// - JPA entities should be simple POJOs with getters/setters
// - Use Lombok @Data to reduce boilerplate
// - Always include database constraints at entity level
// - Use @CreationTimestamp and @UpdateTimestamp for audit fields
// ============================================================================

package com.adminportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity - Represents application users
 * DATABASE TABLE: users
 * RELATIONSHIPS: One-to-Many with RefreshToken
 * AUDIT: created_at, updated_at timestamps tracked automatically
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_users_email"),
        @UniqueConstraint(columnNames = "username", name = "uk_users_username")
    },
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_role", columnList = "role")
    }
)
@Data  // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor  // Lombok: generates no-arg constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
@Builder  // Lombok: generates builder pattern
public class User {


    
    /** ==================== PRIMARY KEY ====================
     * Unique identifier - UUID format
     * Generated automatically by database
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;


    
    /** ==================== AUTHENTICATION FIELDS ====================
     * User email address - UNIQUE, required for login
     * Used as secondary login identifier
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    /**
     * Username - UNIQUE, used for profile identification
     * Minimum 3 characters, maximum 20
     */
    @Column(name = "username", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * Password hash - stored securely using bcrypt algorithm
     * Never store plaintext passwords
     * Minimum 8 characters when creating
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;


    /** ==================== PROFILE FIELDS ====================
     * User first name
     */
    @Column(name = "first_name", nullable = false, length = 50)
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * User last name
     */
    @Column(name = "last_name", nullable = false, length = 50)
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * User phone number - optional
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * User department/organization unit - optional
     */
    @Column(name = "department", length = 100)
    private String department;

 
    /** ==================== AUTHORIZATION FIELDS ==================
     * User role - determines access level and permissions
     * ADMIN: Full system access, can manage all users
     * MANAGER: Can manage team members and view reports
     * USER: Standard user access
     *
     * NOT NULL enforced at database level
     */
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)  // Store as string in DB (e.g., "ADMIN")
    private UserRole role;

    /**
     * User status - determines if account is active
     * ACTIVE: User can login
     * INACTIVE: User exists but cannot login
     * SUSPENDED: User account suspended due to violations
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // ==================== PERMISSION FIELDS ====================
    /**
     * Permission: Can manage other users (admin function)
     * True only for ADMIN and select MANAGER roles
     */
    @Column(name = "can_manage_users", nullable = false)
    private Boolean canManageUsers = false;

    /**
     * Permission: Can view reports and analytics
     * True for MANAGER and ADMIN roles
     */
    @Column(name = "can_view_reports", nullable = false)
    private Boolean canViewReports = false;

    /**
     * Permission: Can manage system settings
     * True only for ADMIN role
     */
    @Column(name = "can_manage_settings", nullable = false)
    private Boolean canManageSettings = false;


    
    /** ==================== AUDIT FIELDS ====================
     * Timestamp when user account was created
     * Set automatically by Hibernate
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when user account was last updated
     * Updated automatically by Hibernate on every save
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp of user's last login
     * Updated manually in AuthService after successful login
     */
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;


    /** ==================== HELPER METHODS ====================
     * Get full display name (concatenated first and last name)
     * Usage: user.getFullName() -> "John Doe"
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if user has admin privileges
     */
    public boolean isAdmin() {
        return UserRole.ADMIN.equals(this.role);
    }

    /**
     * Check if user is active and can login
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}


