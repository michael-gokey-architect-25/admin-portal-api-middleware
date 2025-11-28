// admin-portal-api/src/main/java/com/adminportal/security/JwtAuthenticationFilter.java

// ============================================================================
// PURPOSE: Filter that extracts and validates JWT tokens from requests
// - Runs on every HTTP request
// - Extracts JWT from Authorization header
// - Validates token and sets Spring Security context
// - Allows request to proceed if token valid
// 1. OncePerRequestFilter: Ensures filter runs only once per request
// 2. Authorization Header: "Bearer <token>" format
// 3. SecurityContext: Spring Security context (set current user)
// 4. Filter Chain: Request flows through multiple filters
// ============================================================================

package com.adminportal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter, Extracts JWT from request and sets Spring Security context
 * EXECUTION ORDER:
 * 1. Extract JWT from Authorization header
 * 2. Validate JWT signature and expiration
 * 3. Load user details from database
 * 4. Set Spring Security context (principal)
 * 5. Allow request to proceed to controller  */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Filter method - executed on every HTTP request
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain to proceed
     * @throws ServletException if servlet error occurs
     * @throws IOException if IO error occurs
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract JWT token from Authorization header
            String token = extractTokenFromRequest(request);

            // If token exists and is valid, set Spring Security context
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                String tokenType = tokenProvider.getTokenTypeFromToken(token);

                // Only process ACCESS tokens (not REFRESH tokens)
                if ("ACCESS".equals(tokenType)) {
                    // Load user details from database
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    // Set request details (IP, session ID, etc.)
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set as current authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set Spring Security authentication for user: {}", username);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication: {}", ex.getMessage());
            // Don't stop request processing - let other filters handle it
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     * @param request HTTP request
     * @return JWT token string or null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }
}

