
// admin-portal-api/src/main/java/com/adminportal/service/UserService.java

// ============================================================================
// PURPOSE: User management service - CRUD operations, search, filtering
// - Get all users (paginated, filtered, sorted)
// - Get single user by ID
// - Create new user (admin only)
// - Update user information
// - Delete user (admin only)
// - Search users by name/email
// - Bulk delete users

// KEY CONCEPTS:
// 1. Separation of Concerns: Service handles business logic only
// 2. Pagination: Handle large datasets efficiently
// 3. Filtering & Sorting: Flexible query capabilities
// 4. Authorization: Service doesn't check auth (controller does with @PreAuthorize)
// 5. DTOs: Service returns DTOs, not entities (loose coupling)
// TEACHING NOTES:
// - Repositories handle data access (queries)
// - Services orchestrate business logic (multiple repos, calculations)
// - Always validate user input before database operations
// - Use specifications/criteria API for complex filtering
// - Paginate large result sets to prevent memory issues
// ============================================================================

package com.adminportal.service;

import com.adminportal.dto.request.CreateUserRequest;
import com.adminportal.dto.request.UpdateUserRequest;
import com.adminportal.dto.response.BulkDeleteResponse;
import com.adminportal.dto.response.UserListResponse;
import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.User;
import com.adminportal.entity.UserRole;
import com.adminportal.entity.UserStatus;
import com.adminportal.exception.DuplicateResourceException;
import com.adminportal.exception.ResourceNotFoundException;
import com.adminportal.exception.ValidationException;
import com.adminportal.mapper.UserMapper;
import com.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Management Service
 * RESPONSIBILITIES:
 * - CRUD operations on users
 * - Search and filtering
 * - Bulk operations
 * - User validation
 * - Permission management
 * WORKFLOW:
 * 1. Controller receives request
 * 2. Spring Security validates authentication (@PreAuthorize)
 * 3. Controller calls service method
 * 4. Service validates input
 * 5. Service calls repository methods
 * 6. Repository queries database
 * 7. Service maps entity to DTO
 * 8. Controller returns response to client  */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    // ==================== DEPENDENCIES ====================
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /** ================== GET ALL USERS (PAGINATED/FILTERED)
     * Get all users with pagination, filtering, and sorting
     * PARAMETERS:
     * - page: 1-indexed page number (1 = first page)
     * - limit: items per page (10, 20, etc.)
     * - role: filter by role (ADMIN, MANAGER, USER)
     * - status: filter by status (ACTIVE, INACTIVE, SUSPENDED)
     * - sortBy: field to sort (name, email, role, joinedDate)
     * - sortOrder: sort direction (asc, desc)
     * EXAMPLE QUERIES:
     * GET /users?page=1&limit=10 (first 10 users)
     * GET /users?page=2&limit=20&role=MANAGER (second page, managers only)
     * GET /users?status=ACTIVE&sortBy=name&sortOrder=asc (active users, sorted by name)
     * @param page Page number (1-indexed)
     * @param limit Items per page
     * @param role Optional role filter
     * @param status Optional status filter
     * @param sortBy Sort field
     * @param sortOrder Sort direction (asc/desc)
     * @return UserListResponse with paginated users     */
    @Transactional(readOnly = true)
    public UserListResponse getAllUsers(
            int page,
            int limit,
            String role,
            String status,
            String sortBy,
            String sortOrder) {

        log.debug("Fetching users - page: {}, limit: {}, role: {}, status: {}, sortBy: {}, sortOrder: {}",
            page, limit, role, status, sortBy, sortOrder);

        // ==================== STEP 1: VALIDATE PAGINATION ====================
        // Convert 1-indexed to 0-indexed for Spring Data
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 10;  // Max 100 per page

        int pageIndex = page - 1;  // Spring uses 0-indexed pages

        // ==================== STEP 2: BUILD SORT ====================
        // Determine sort direction
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;

        // Map sortBy field names (handle multiple field options)
        String sortField;
        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "email":
                sortField = "email";
                break;
            case "role":
                sortField = "role";
                break;
            case "joineddate":
            case "created_at":
                sortField = "createdAt";
                break;
            case "name":
            default:
                sortField = "firstName";  // Default sort by first name
                break;
        }

        Pageable pageable = PageRequest.of(pageIndex, limit, Sort.by(direction, sortField));

        // ==================== STEP 3: QUERY DATABASE ====================
        // Spring Data provides methods for filtering (generated from entity attributes)
        Page<User> usersPage;

        if (role != null && !role.isBlank() && status != null && !status.isBlank()) {
            // Both filters
            usersPage = userRepository.findByRoleAndStatus(
                UserRole.valueOf(role.toUpperCase()),
                UserStatus.valueOf(status.toUpperCase()),
                pageable
            );
        } else if (role != null && !role.isBlank()) {
            // Role filter only
            usersPage = userRepository.findByRole(
                UserRole.valueOf(role.toUpperCase()),
                pageable
            );
        } else if (status != null && !status.isBlank()) {
            // Status filter only
            usersPage = userRepository.findByStatus(
                UserStatus.valueOf(status.toUpperCase()),
                pageable
            );
        } else {
            // No filters
            usersPage = userRepository.findAll(pageable);
        }

        log.debug("Found {} users (total: {})", usersPage.getContent().size(), usersPage.getTotalElements());

        // ==================== STEP 4: MAP TO DTOS ====================
        List<UserResponse> userResponses = usersPage.getContent()
            .stream()
            .map(userMapper::toUserResponse)
            .collect(Collectors.toList());

        // ==================== STEP 5: BUILD RESPONSE ====================
        return UserListResponse.builder()
            .data(userResponses)
            .pagination(
                UserListResponse.Pagination.builder()
                    .page(page)
                    .limit(limit)
                    .total((int) usersPage.getTotalElements())
                    .totalPages(usersPage.getTotalPages())
                    .build()
            )
            .build();
    }

   
 
    /** ==================== GET SINGLE USER ====================
     * Get single user by ID
     * @param userId User UUID
     * @return UserResponse with user data
     * @throws ResourceNotFoundException if user not found   */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user by ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found: {}", userId);
                return new ResourceNotFoundException("User not found");
            });

        return userMapper.toUserResponse(user);
    }

    
 
    /** ==================== SEARCH USERS ====================
     * Search users by name, email, or username
     * - Search for "alice" → finds alice@example.com, Alice User
     * - Search for "john@" → finds john@example.com
     * - Search for "john.doe" → finds username john.doe
     * @param searchTerm Text to search for
     * @param limit Max results (default 20)
     * @return List of matching users     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm, int limit) {
        log.debug("Searching users with term: {}, limit: {}", searchTerm, limit);

        if (searchTerm == null || searchTerm.isBlank()) {
            log.warn("Search term is blank");
            throw new ValidationException("Search term cannot be empty");
        }

        if (limit < 1 || limit > 100) limit = 20;

        // ==================== SEARCH LOGIC ====================
        // Find users matching first name, last name, email, or username
        // Case-insensitive LIKE search
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";

        List<User> results = userRepository.findBySearchTerm(searchTerm, limit);

        log.debug("Found {} matching users", results.size());

        return results.stream()
            .map(userMapper::toUserResponse)
            .collect(Collectors.toList());
    }

    
 
    /** ==================== CREATE USER ====================
     * Create new user (admin only) BUSINESS RULES:
     * - Email must be unique
     * - Username must be unique
     * - Password must be at least 8 characters
     * - New users default to USER role (admin can override)
     * @param createUserRequest User data from request
     * @return UserResponse with created user
     * @throws DuplicateResourceException if email/username exists
     * @throws ValidationException if validation fails */
    
    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        log.info("Creating new user: {}", createUserRequest.getEmail());

        // ==================== STEP 1: VALIDATE INPUT ====================
        validateUserInput(
            createUserRequest.getFirstName(),
            createUserRequest.getLastName(),
            createUserRequest.getEmail(),
            createUserRequest.getPassword()
        );

        // ==================== STEP 2: CHECK EMAIL UNIQUENESS ====================
        if (userRepository.findByEmail(createUserRequest.getEmail()).isPresent()) {
            log.warn("Email already exists: {}", createUserRequest.getEmail());
            throw new DuplicateResourceException("Email already registered");
        }

        // ==================== STEP 3: CHECK USERNAME UNIQUENESS ====================
        if (userRepository.findByUsername(createUserRequest.getUsername()).isPresent()) {
            log.warn("Username already exists: {}", createUserRequest.getUsername());
            throw new DuplicateResourceException("Username already taken");
        }

        // ==================== STEP 4: HASH PASSWORD ====================
        String hashedPassword = passwordEncoder.encode(createUserRequest.getPassword());

        // ==================== STEP 5: CREATE USER ENTITY ====================
        User newUser = User.builder()
            .email(createUserRequest.getEmail())
            .username(createUserRequest.getUsername())
            .password(hashedPassword)
            .firstName(createUserRequest.getFirstName())
            .lastName(createUserRequest.getLastName())
            .phone(createUserRequest.getPhone())
            .department(createUserRequest.getDepartment())
            .role(createUserRequest.getRole())
            .status(createUserRequest.getStatus())
            .canManageUsers(createUserRequest.getCanManageUsers() != null && 
                            createUserRequest.getCanManageUsers())
            .canViewReports(createUserRequest.getCanViewReports() != null && 
                            createUserRequest.getCanViewReports())
            .canManageSettings(createUserRequest.getCanManageSettings() != null && 
                              createUserRequest.getCanManageSettings())
            .build();

        // ==================== STEP 6: SAVE TO DATABASE ====================
        User savedUser = userRepository.save(newUser);
        log.info("User created successfully: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }



    /** ========== UPDATE USER COMING ================ */
    /**
     * Update user information, BUSINESS RULES:
     * - Can only update own profile (unless admin)
     * - Admins can update any user and change roles/permissions
     * - Email/username uniqueness enforced
     * - Password update hashes new password
     * - Partial updates supported (only provided fields updated)
     *
     * @param userId User ID to update
     * @param updateRequest Fields to update
     * @param currentUserId Currently authenticated user ID
     * @param isAdmin Is current user admin?
     * @return UserResponse with updated user
     * @throws ResourceNotFoundException if user not found
     * @throws DuplicateResourceException if email/username exists   */
    @Transactional
    public UserResponse updateUser(
            UUID userId,
            UpdateUserRequest updateRequest,
            UUID currentUserId,
            boolean isAdmin) {

        log.info("Updating user: {} (requestedBy: {})", userId, currentUserId);

        // ==================== STEP 1: AUTHORIZATION CHECK ====================
        // Users can only update themselves (admin can update anyone)
        if (!userId.equals(currentUserId) && !isAdmin) {
            log.warn("Unauthorized update attempt - user {} tried to update user {}",
                currentUserId, userId);
            throw new ResourceNotFoundException("Cannot update other user's profile");
        }

        // ==================== STEP 2: FIND USER ====================
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found for update: {}", userId);
                return new ResourceNotFoundException("User not found");
            });

        // ==================== STEP 3: UPDATE FIELDS ====================
        // Only update fields that are provided (null = no change)

        if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().isBlank()) {
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null && !updateRequest.getLastName().isBlank()) {
            user.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }

        if (updateRequest.getDepartment() != null) {
            user.setDepartment(updateRequest.getDepartment());
        }

        // ==================== STEP 4: UPDATE EMAIL (ADMIN ONLY) ====================
        // Email change requires uniqueness check
        if (updateRequest.getEmail() != null && 
            !updateRequest.getEmail().isBlank() &&
            !updateRequest.getEmail().equals(user.getEmail())) {

            // Check if admin is trying to change someone else's email
            if (!isAdmin && !userId.equals(currentUserId)) {
                throw new ResourceNotFoundException("Cannot change other user's email");
            }

            // Check uniqueness
            if (userRepository.findByEmail(updateRequest.getEmail()).isPresent()) {
                log.warn("Email already exists: {}", updateRequest.getEmail());
                throw new DuplicateResourceException("Email already registered");
            }

            user.setEmail(updateRequest.getEmail());
        }

        // ==================== STEP 5: UPDATE PASSWORD ====================
        // Password change hashes new value
        if (updateRequest.getPassword() != null && 
            !updateRequest.getPassword().isBlank() &&
            updateRequest.getPassword().length() >= 8) {

            String hashedPassword = passwordEncoder.encode(updateRequest.getPassword());
            user.setPassword(hashedPassword);
            log.debug("Password updated for user: {}", userId);
        }

        // ==================== STEP 6: UPDATE ROLE & STATUS (ADMIN ONLY) ====================
        if (isAdmin) {
            if (updateRequest.getRole() != null) {
                user.setRole(updateRequest.getRole());
                log.debug("Role updated for user: {} to {}", userId, updateRequest.getRole());
            }

            if (updateRequest.getStatus() != null) {
                user.setStatus(updateRequest.getStatus());
                log.debug("Status updated for user: {} to {}", userId, updateRequest.getStatus());
            }

            // ==================== STEP 7: UPDATE PERMISSIONS (ADMIN ONLY) ====================
            if (updateRequest.getCanManageUsers() != null) {
                user.setCanManageUsers(updateRequest.getCanManageUsers());
            }

            if (updateRequest.getCanViewReports() != null) {
                user.setCanViewReports(updateRequest.getCanViewReports());
            }

            if (updateRequest.getCanManageSettings() != null) {
                user.setCanManageSettings(updateRequest.getCanManageSettings());
            }
        }

        // ==================== STEP 8: UPDATE TIMESTAMP ====================
        user.setUpdatedAt(LocalDateTime.now());

        // ==================== STEP 9: SAVE ====================
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);

        return userMapper.toUserResponse(updatedUser);
    }


 
    /** ==================== DELETE USER ====================
     * Delete user (admin only)
     * - User record from database
     * - All refresh tokens (cascade delete)
     * - User cannot be recovered (hard delete)
     *
     * @param userId User ID to delete
     * @throws ResourceNotFoundException if user not found   */
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("Deleting user: {}", userId);

        // ==================== STEP 1: FIND USER ====================
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found for deletion: {}", userId);
                return new ResourceNotFoundException("User not found");
            });

        // ==================== STEP 2: DELETE ====================
        // ON DELETE CASCADE in database will delete associated refresh tokens
        userRepository.delete(user);
        log.info("User deleted successfully: {} (ID: {})", user.getEmail(), userId);
    }



     
    /** ==================== BULK DELETE ====================
     * Delete multiple users at once (admin only)
     * - Efficient deletion of multiple users
     * - Tracks successful and failed deletions
     * - Returns summary of operation
     * @param userIds List of user IDs to delete
     * @return BulkDeleteResponse with deletion summary    */
    @Transactional
    public BulkDeleteResponse bulkDeleteUsers(List<UUID> userIds) {
        log.info("Bulk deleting {} users", userIds.size());

        int deletedCount = 0;
        int failedCount = 0;

        // ==================== ITERATE & DELETE ====================
        for (UUID userId : userIds) {
            try {
                deleteUser(userId);
                deletedCount++;
            } catch (Exception ex) {
                log.error("Failed to delete user {}: {}", userId, ex.getMessage());
                failedCount++;
            }
        }

        // ==================== BUILD RESPONSE ====================
        String message = String.format(
            "%d users deleted successfully%s",
            deletedCount,
            failedCount > 0 ? String.format(", %d failed", failedCount) : ""
        );

        log.info("Bulk delete completed: {} deleted, {} failed", deletedCount, failedCount);

        return BulkDeleteResponse.builder()
            .deletedCount(deletedCount)
            .failedCount(failedCount)
            .message(message)
            .build();
    }



     
    /** ==================== HELPER METHODS ====================
     * Validate user input fields during create/update operations
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param password Password (can be null for updates)
     * @throws ValidationException if validation fails     */
    private void validateUserInput(String firstName, String lastName, String email, String password) {
        if (firstName == null || firstName.isBlank() || firstName.length() < 2) {
            throw new ValidationException("First name must be at least 2 characters");
        }

        if (lastName == null || lastName.isBlank() || lastName.length() < 2) {
            throw new ValidationException("Last name must be at least 2 characters");
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Valid email is required");
        }

        if (password != null && password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
    }


    /**
     * Get user profile with permissions
     *
     * @param userId User ID
     * @return User data with permissions
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        log.debug("Getting user profile: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toUserResponse(user);
    }
}


    
