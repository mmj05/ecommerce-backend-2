package com.ecommerce.ecom.util;

import com.ecommerce.ecom.exceptions.APIException;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {
    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);

    @Autowired
    UserRepository userRepository;

    public String loggedInEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.error("No authenticated user found");
            throw new APIException("User not authenticated");
        }

        logger.info("Looking up user by username: {}", authentication.getName());
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", authentication.getName());
                    return new UsernameNotFoundException("User Not Found with username: " + authentication.getName());
                });

        return user.getEmail();
    }

    public Long loggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.error("No authenticated user found");
            throw new APIException("User not authenticated");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", authentication.getName());
                    return new UsernameNotFoundException("User Not Found with username: " + authentication.getName());
                });

        return user.getUserId();
    }

    public User loggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            logger.error("No authenticated user found");
            throw new APIException("User not authenticated");
        }

        logger.info("Getting logged in user: {}", authentication.getName());
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", authentication.getName());
                    return new UsernameNotFoundException("User Not Found with username: " + authentication.getName());
                });

        return user;
    }
}