// FIXED src/main/java/com/ecommerce/ecom/controller/UserProfileController.java
package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.exceptions.APIException;
import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.payload.APIResponse;
import com.ecommerce.ecom.payload.UserProfileUpdateDTO;
import com.ecommerce.ecom.payload.PasswordChangeDTO;
import com.ecommerce.ecom.repositories.UserRepository;
import com.ecommerce.ecom.security.services.UserDetailsImpl;
import com.ecommerce.ecom.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile") // Changed from /api/auth to /api/profile
public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthUtil authUtil;

    // Update user email
    @PutMapping("/email")  // Changed from /user/email to just /email
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateEmail(@RequestBody UserProfileUpdateDTO profileUpdateDTO) {
        try {
            User user = authUtil.loggedInUser();
            if (user == null) {
                return new ResponseEntity<>(new APIResponse("User not found", false), HttpStatus.NOT_FOUND);
            }

            // Validate the email
            if (profileUpdateDTO.getEmail() == null || profileUpdateDTO.getEmail().trim().isEmpty()) {
                return new ResponseEntity<>(new APIResponse("Email cannot be empty", false), HttpStatus.BAD_REQUEST);
            }

            // Check if email is already in use by another user
            if (userRepository.existsByEmail(profileUpdateDTO.getEmail()) && !user.getEmail().equals(profileUpdateDTO.getEmail())) {
                return new ResponseEntity<>(new APIResponse("Email is already in use", false), HttpStatus.BAD_REQUEST);
            }

            // Update the email
            user.setEmail(profileUpdateDTO.getEmail());
            userRepository.save(user);

            return new ResponseEntity<>(new APIResponse("Email updated successfully", true), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating email: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error updating email: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Change password
    @PutMapping("/password") // Changed from /user/password to just /password
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO) {
        try {
            User user = authUtil.loggedInUser();
            if (user == null) {
                return new ResponseEntity<>(new APIResponse("User not found", false), HttpStatus.NOT_FOUND);
            }

            // Validate the passwords
            if (passwordChangeDTO.getCurrentPassword() == null || passwordChangeDTO.getCurrentPassword().isEmpty()) {
                return new ResponseEntity<>(new APIResponse("Current password is required", false), HttpStatus.BAD_REQUEST);
            }

            if (passwordChangeDTO.getNewPassword() == null || passwordChangeDTO.getNewPassword().isEmpty()) {
                return new ResponseEntity<>(new APIResponse("New password is required", false), HttpStatus.BAD_REQUEST);
            }

            if (passwordChangeDTO.getNewPassword().length() < 6) {
                return new ResponseEntity<>(new APIResponse("New password must be at least 6 characters", false), HttpStatus.BAD_REQUEST);
            }

            // Verify current password
            if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), user.getPassword())) {
                return new ResponseEntity<>(new APIResponse("Current password is incorrect", false), HttpStatus.BAD_REQUEST);
            }

            // Update password
            user.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
            userRepository.save(user);

            return new ResponseEntity<>(new APIResponse("Password changed successfully", true), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error changing password: {}", e.getMessage(), e);
            return new ResponseEntity<>(new APIResponse("Error changing password: " + e.getMessage(), false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get current user details - REMOVED to avoid conflict with AuthController
    // We will use the existing endpoint in AuthController instead
}