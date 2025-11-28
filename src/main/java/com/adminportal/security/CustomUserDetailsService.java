// admin-portal-api/src/main/java/com/adminportal/security/CustomUserDetailsService.java

// ============================================================================
// PURPOSE: Load user details from database for Spring Security
// - Implements UserDetailsService interface
// - Converts User entity to Spring UserDetails
// - Used during authentication and authorization
// 1. UserDetailsService: Spring interface for loading user data
// 2. UserDetails: Spring interface representing authenticated user
// 3. GrantedAuthority: User roles/permissions
// 4. Username principal: Identifier used by Spring Security
// ============================================================================

package com.adminportal.security;

import com.adminportal.entity.User;
import com.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Custom User Details Service
 * Loads user from database and converts to Spring UserDetails
 * This service is called by:
 * - AuthenticationManager during login
 * - JwtAuthenticationFilter during authorization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username/email
     * Called by Spring Security during authentication
     *
     * @param username Username or email (client can use either)
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        // Try to find by username first, then by email
        User user = userRepository.findByUsername(username)
            .orElseGet(() -> userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                }));

        // Convert User entity to Spring UserDetails
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            true,                              // Account enabled
            true,                              // Account not expired
            true,                              // Credentials not expired
            user.isActive(),                   // Account not locked (checks status)
            getAuthorities(user)               // User roles/permissions
        );
    }

    /**
     * Convert User entity roles to Spring GrantedAuthority collection
     * Used for authorization decisions
     * @param user User entity from database
     * @return Collection of user authorities (roles)
     */
    private Collection<GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Add role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Add permission authorities if granted
        if (Boolean.TRUE.equals(user.getCanManageUsers())) {
            authorities.add(new SimpleGrantedAuthority("MANAGE_USERS"));
        }
        if (Boolean.TRUE.equals(user.getCanViewReports())) {
            authorities.add(new SimpleGrantedAuthority("VIEW_REPORTS"));
        }
        if (Boolean.TRUE.equals(user.getCanManageSettings())) {
            authorities.add(new SimpleGrantedAuthority("MANAGE_SETTINGS"));
        }

        return authorities;
    }
}

