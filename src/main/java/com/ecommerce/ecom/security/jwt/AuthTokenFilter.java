package com.ecommerce.ecom.security.jwt;

import com.ecommerce.ecom.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.debug("AuthTokenFilter called for URI: {} Method: {}", request.getRequestURI(), request.getMethod());

        try {
            String jwt = parseJwt(request);
            logger.debug("Extracted JWT: {}", jwt != null ? "Present" : "Not found");

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                logger.debug("JWT is valid for user: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.debug("User details loaded. Authorities: {}", userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails,
                                null,
                                userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Authentication set for user: {} with roles: {}", username, userDetails.getAuthorities());
            } else {
                logger.debug("JWT is null or invalid for URI: {}", request.getRequestURI());
                // Clear any existing authentication
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication for URI: {} - Error: {}", request.getRequestURI(), e.getMessage());
            // Clear context on error
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromCookies(request);

        // Additional debugging for cookie extraction
        if (jwt == null) {
            logger.debug("No JWT cookie found. All cookies: {}",
                    request.getCookies() != null ?
                            java.util.Arrays.toString(request.getCookies()) : "No cookies");
        }

        return jwt;
    }
}