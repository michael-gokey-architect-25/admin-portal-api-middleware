// admin-portal-api/src/main/java/com/adminportal/repository/UserRepository.java

// ============================================================================
// PURPOSE: Spring Data JPA repository for User entity
// - Extends JpaRepository for basic CRUD operations
// - Defines custom query methods
// - No implementation needed (Spring generates it)
// KEY CONCEPTS:
// 1. Repository Pattern: Abstracts data access logic
// 2. Spring Data JPA: Auto-generates JDBC/SQL from method names
// 3. Custom Queries: @Query for complex operations
// 4. Derived Methods: findByEmail generates SQL automatically
// TEACHING NOTES:
// - Spring Data generates SQL based on method names
// - Method: findByEmailAndRole → SELECT * FROM users WHERE email = ? AND role = ?
// - Method: findByRoleOrderByFirstName → SELECT * FROM users WHERE role = ? ORDER BY first_name
// - Use @Query for complex queries that method names can't express
// - Repositories are interfaces, Spring creates proxy implementations
// ============================================================================

package com.adminportal.repository;

import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD METHODS (inherited from JpaRepository):
 * - findAll() → SELECT * FROM users
 * - findById(id) → SELECT * FROM users WHERE id = ?
 * - save(user) → INSERT or UPDATE
 * - delete(user) → DELETE
 * - count() → SELECT COUNT(*) FROM users
 * CUSTOM METHODS (defined below):
 * - findByEmail(email) → SELECT * FROM users WHERE email = ?
 * - findByRole(role, pageable) → SELECT * FROM users WHERE role = ? (paginated)
 * - Custom @Query methods for complex scenarios
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** ========= FINDING BY SINGLE FIELD ============
     * Find user by email (case-sensitive)
     * Used for login, registration validation
     *
     * SQL Generated:
     * SELECT * FROM users WHERE email = ?
     *
     * @param email User email address
     * @return Optional containing user if found     */
    Optional<User> findByEmail(String email);


    /**
     * Find user by username (case-sensitive)
     * Used for display name uniqueness, user search
     *
     * SQL Generated:
     * SELECT * FROM users WHERE username = ?
     *
     * @param username User username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);



     
    /** ============= PAGINATION & FILTERING ================
     * Find all users of specific role (paginated)
     * Used for listing managers, admins, etc.
     *
     * SQL Generated:
     * SELECT * FROM users WHERE role = ? ORDER BY [pagination params]
     *
     * @param role User role to filter
     * @param pageable Pagination information
     * @return Page of users with pagination metadata     */
    Page<User> findByRole(UserRole role, Pageable pageable);


    /**
     * Find all users with specific status (paginated)
     * Used for active/inactive user lists
     *
     * SQL Generated:
     * SELECT * FROM users WHERE status = ?
     *
     * @param status User account status
     * @param pageable Pagination information
     * @return Page of matching users
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);


    /**
     * Find users by role AND status (paginated)
     * Used for complex filtering (e.g., active admins)
     *
     * SQL Generated:
     * SELECT * FROM users WHERE role = ? AND status = ?
     *
     * @param role User role
     * @param status User status
     * @param pageable Pagination information
     * @return Page of matching users
     */
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);



    
    /** ============== CUSTOM @QUERY METHODS ================
     * Search users by name, email, or username
     * Case-insensitive LIKE search
     *
     * SQL:
     * SELECT u FROM User u WHERE
     *   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
     *   LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
     *   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
     *   LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
     * ORDER BY u.firstName
     * LIMIT :limit
     *
     * @param searchTerm Text to search for
     * @param limit Maximum number of results
     * @return List of matching users      */
    @Query(
        value = "SELECT u FROM User u WHERE " +
                "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "ORDER BY u.firstName",
        countQuery = "SELECT COUNT(u) FROM User u WHERE " +
                    "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                    "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    List<User> findBySearchTerm(
        @Param("searchTerm") String searchTerm,
        @Param("limit") int limit
    );



    /**
     * Check if user exists by email
     * More efficient than findByEmail when only need boolean
     *
     * SQL Generated:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     *
     * @param email User email
     * @return true if user with email exists
     */
    boolean existsByEmail(String email);



    /**
     * Check if user exists by username
     *
     * SQL Generated:
     * SELECT COUNT(*) > 0 FROM users WHERE username = ?
     *
     * @param username User username
     * @return true if user with username exists
     */
    boolean existsByUsername(String username);
}

