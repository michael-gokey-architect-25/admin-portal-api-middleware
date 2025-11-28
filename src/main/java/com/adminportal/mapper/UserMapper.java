// admin-portal-api/src/main/java/com/adminportal/mapper/UserMapper.java

// ============================================================================
// PURPOSE: MapStruct mapper - convert User entity to UserResponse DTO
// - Automatic mapping with compile-time code generation
// - Type-safe, null-safe conversions
// - Replaces manual mapping or model mapper
// KEY CONCEPTS:
// 1. MapStruct: Generate mapping code at compile time (not runtime)
// 2. DTO Pattern: Separate API contract from database model
// 3. Loose Coupling: Changes to entity don't break API
// 4. Null Handling: Configure null checking strategies
// TEACHING NOTES:
// - MapStruct generates Java code, not reflection
// - Performance: As fast as manual mapping
// - Compile errors: If mapping impossible, fails at compile time
// - Custom mapping: Can add custom logic for complex fields
// ============================================================================

package com.adminportal.mapper;

import com.adminportal.dto.response.UserResponse;
import com.adminportal.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * User Entity to DTO Mapper,  MAPPINGS:
 * - User.createdAt → UserResponse.joinedDate
 * - User.password → (excluded - never send password)
 * - All other fields → same name (automatic)
 * GENERATED CODE:
 * MapStruct generates the implementation at compile time:
 * 
 * public UserResponse toUserResponse(User user) {
 *     if (user == null) return null;
 *     UserResponse response = new UserResponse();
 *     response.setId(user.getId());
 *     response.setFirstName(user.getFirstName());
 *     ... (all fields)
 *     return response;
 * }
 */
@Mapper(componentModel = "spring")  // Spring component for autowiring
public interface UserMapper {

    /**
     * Map User entity to UserResponse DTO
     *
     * FIELD MAPPINGS:
     * - createdAt → joinedDate (rename)
     * - password → (not mapped - security)
     * - All others → same name (automatic)
     *
     * @param user User entity from database
     * @return UserResponse DTO for API response
     */
    @Mapping(source = "createdAt", target = "joinedDate")
    @Mapping(target = "password", ignore = true)  // Never include password
    UserResponse toUserResponse(User user);


    /**
     * Map UserResponse DTO back to User entity
     * Used rarely (usually create/update separately)
     *
     * @param userResponse DTO from API request
     * @return User entity for database
     */
    @Mapping(source = "joinedDate", target = "createdAt")
    @Mapping(target = "password", ignore = true)  // Never set password from response
    User toUser(UserResponse userResponse);
}

