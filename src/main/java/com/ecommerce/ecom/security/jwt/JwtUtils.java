package com.ecommerce.ecom.security.jwt;

import com.ecommerce.ecom.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${spring.app.jwtExpirationMsExtended}")
    private long jwtExpirationMsExtended;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if (cookie != null) {
            logger.debug("Found JWT cookie: {}", cookie.getValue());
            return cookie.getValue();
        } else {
            logger.debug("No JWT cookie found");
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        return generateJwtCookie(userPrincipal, jwtExpirationMs);
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal, long expirationMs) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername(), expirationMs);
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, jwt)
                .path("/")  // Changed from "/api" to "/" to cover all paths
                .maxAge(expirationMs / 1000) // Convert from ms to seconds
                .httpOnly(true)
                .secure(false)  // Changed to false for localhost HTTP development
                .sameSite("Lax")  // Changed from "Strict" to "Lax" for better cross-origin support
                .build();

        logger.info("Generated JWT cookie: name={}, path=/, secure=false, sameSite=Lax, maxAge={} seconds",
                jwtCookie, expirationMs / 1000);
        return cookie;
    }

    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, null)
                .path("/")  // Changed from "/api" to "/"
                .maxAge(0)  // Explicitly set maxAge to 0 for immediate expiration
                .httpOnly(true)
                .secure(false)  // Match the generation settings
                .sameSite("Lax")
                .build();
        return cookie;
    }

    public String generateTokenFromUsername(String username) {
        return generateTokenFromUsername(username, jwtExpirationMs);
    }

    public String generateTokenFromUsername(String username, long expirationMs) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + expirationMs))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}